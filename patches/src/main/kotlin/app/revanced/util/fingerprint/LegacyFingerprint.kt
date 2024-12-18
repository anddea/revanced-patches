@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package app.revanced.util.fingerprint

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.Match
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstruction
import app.revanced.util.injectLiteralInstructionViewCall
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
        val targetIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
        val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

        val smaliInstruction =
            if (descriptor.startsWith("0x")) """
                const/16 v$targetRegister, $descriptor
                """
            else if (descriptor.endsWith("(Z)Z")) """
                invoke-static {v$targetRegister}, $descriptor
                move-result v$targetRegister
                """
            else """
                invoke-static {}, $descriptor
                move-result v$targetRegister
                """

        addInstructions(
            targetIndex + 1,
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

internal fun legacyFingerprint(
    name: String,
    accessFlags: Int? = null,
    returnType: String? = null,
    parameters: List<String>? = null,
    opcodes: List<Opcode?>? = null,
    strings: List<String>? = null,
    literals: List<Long>? = null,
    customFingerprint: ((methodDef: Method, classDef: ClassDef) -> Boolean)? = null
) = Pair(
    name,
    fingerprint {
        if (accessFlags != null) {
            accessFlags(accessFlags)
        }
        if (returnType != null) {
            returns(returnType)
        }
        if (parameters != null) {
            parameters(*parameters.toTypedArray())
        }
        if (opcodes != null) {
            opcodes(*opcodes.toTypedArray())
        }
        if (strings != null) {
            strings(*strings.toTypedArray())
        }
        custom { method, classDef ->
            if (literals != null) {
                for (literal in literals)
                    if (!method.containsLiteralInstruction(literal))
                        return@custom false
            }
            if (customFingerprint != null && !customFingerprint(method, classDef)) {
                return@custom false
            }

            return@custom true
        }
    }
)

