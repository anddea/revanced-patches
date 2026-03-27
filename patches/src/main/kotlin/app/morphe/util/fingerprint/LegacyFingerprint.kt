@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package app.morphe.util.fingerprint

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.Match
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstruction
import app.morphe.util.injectLiteralInstructionViewCall
import com.android.tools.smali.dexlib2.AccessFlags.getAccessFlagsForMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val String.exception
    get() = PatchException("Failed to resolve $this")

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.resolvable(): Boolean =
    second.methodOrNull != null

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.definingClassOrThrow(): String =
    second.classDefOrNull?.type ?: throw first.exception

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrThrow(): Match =
    second.match(mutableClassOrThrow())

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrThrow(parentFingerprint: Pair<String, Fingerprint>): Match {
    val parentClassDef = parentFingerprint.second.classDefOrNull
        ?: throw parentFingerprint.first.exception
    return second.matchOrNull(parentClassDef)
        ?: throw first.exception
}

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrNull(): Match? =
    second.classDefOrNull?.let {
        second.matchOrNull(it)
    }

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrNull(parentFingerprint: Pair<String, Fingerprint>): Match? =
    parentFingerprint.second.classDefOrNull?.let { parentClassDef ->
        second.matchOrNull(parentClassDef)
    }

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrNull(): MutableMethod? =
    matchOrNull()?.method

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrThrow(): MutableMethod =
    second.methodOrNull ?: throw first.exception

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrThrow(parentFingerprint: Pair<String, Fingerprint>): MutableMethod =
    matchOrThrow(parentFingerprint).method

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.originalMethodOrThrow(): Method =
    second.originalMethodOrNull ?: throw first.exception

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.originalMethodOrThrow(parentFingerprint: Pair<String, Fingerprint>): Method =
    matchOrThrow(parentFingerprint).originalMethod

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.mutableClassOrThrow(): MutableClass =
    second.classDefOrNull ?: throw first.exception

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodCall() =
    methodOrThrow().methodCall()

context(BytecodePatchContext)
internal fun MutableMethod.methodCall(): String {
    var methodCall = "$definingClass->$name("
    for (i in 0 until parameters.size) {
        methodCall += parameterTypes[i]
    }
    methodCall += ")$returnType"
    return methodCall
}

context(BytecodePatchContext)
fun Pair<String, Fingerprint>.injectLiteralInstructionBooleanCall(
    literal: Long,
    descriptor: String
) {
    methodOrThrow().apply {
        val literalIndex = indexOfFirstLiteralInstruction(literal)
        val index = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        val smaliInstruction =
            if (descriptor.startsWith("0x")) """
                const/16 v$register, $descriptor
                """
            else if (descriptor.endsWith("(Z)Z")) """
                invoke-static/range { v$register .. v$register }, $descriptor
                move-result v$register
                """
            else """
                invoke-static {}, $descriptor
                move-result v$register
                """

        addInstructions(
            index + 1,
            smaliInstruction
        )
    }
}

context(BytecodePatchContext)
fun Pair<String, Fingerprint>.injectLiteralInstructionViewCall(
    literal: Long,
    smaliInstruction: String
) {
    val method = methodOrThrow()
    method.injectLiteralInstructionViewCall(literal, smaliInstruction)
}

@Deprecated("Migrate away from fingerprint DSL")
internal fun legacyFingerprint(
    name: String,
    accessFlags: Int? = null,
    returnType: String? = null,
    parameters: List<String>? = null,
    opcodes: List<Opcode?>? = null,
    strings: List<String>? = null,
    literals: List<Long>? = null,
    customFingerprint: ((methodDef: Method, classDef: ClassDef) -> Boolean)? = null,
) = Pair(
    name,
    Fingerprint(
        accessFlags = if (accessFlags != null) getAccessFlagsForMethod(accessFlags).toList() else null,
        returnType = returnType,
        parameters = parameters,
        strings = strings,
        filters = if (opcodes != null) OpcodesFilter.opcodesToFilters(*opcodes.toTypedArray()) else null,
        custom = { method, classDef ->
            if (literals != null) {
                for (literal in literals)
                    if (!method.containsLiteralInstruction(literal))
                        return@Fingerprint false
            }
            customFingerprint == null || customFingerprint(method, classDef)
        }
    )
)
