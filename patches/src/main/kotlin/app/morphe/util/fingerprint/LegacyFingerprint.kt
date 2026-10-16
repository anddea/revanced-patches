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
import app.morphe.util.Utils.printWarn
import app.morphe.util.forEachInlinedFeatureFlagSite
import com.android.tools.smali.dexlib2.AccessFlags.getAccessFlagsForMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val String.exception
    get() = PatchException("Failed to resolve $this")

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.resolvable(): Boolean =
    second.methodOrNull != null

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.definingClassOrThrow(): String =
    second.classDefOrNull?.type ?: throw first.exception

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrThrow(): Match =
    second.match(mutableClassOrThrow())

/**
 * Resolves a fingerprint only when it matches exactly one method.
 *
 * The regular [Fingerprint.method] accessor selects the first match. Requiring a single match
 * prevents a patch from silently modifying the wrong method when obfuscated targets contain
 * multiple candidates.
 */
context(_: BytecodePatchContext)
internal fun Fingerprint.matchSingle(): Match =
    matchAll(1..1).first()

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrThrow(parentFingerprint: Pair<String, Fingerprint>): Match {
    val parentClassDef = parentFingerprint.second.classDefOrNull
        ?: throw parentFingerprint.first.exception
    return second.matchOrNull(parentClassDef)
        ?: throw first.exception
}

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrNull(): Match? =
    second.classDefOrNull?.let {
        second.matchOrNull(it)
    }

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrNull(parentFingerprint: Pair<String, Fingerprint>): Match? =
    parentFingerprint.second.classDefOrNull?.let { parentClassDef ->
        second.matchOrNull(parentClassDef)
    }

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrNull(): MutableMethod? =
    matchOrNull()?.method

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrThrow(): MutableMethod =
    second.methodOrNull ?: throw first.exception

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrThrow(parentFingerprint: Pair<String, Fingerprint>): MutableMethod =
    matchOrThrow(parentFingerprint).method

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.originalMethodOrThrow(): Method =
    second.originalMethodOrNull ?: throw first.exception

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.originalMethodOrThrow(parentFingerprint: Pair<String, Fingerprint>): Method =
    matchOrThrow(parentFingerprint).originalMethod

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.mutableClassOrThrow(): MutableClass =
    second.classDefOrNull ?: throw first.exception

context(_: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodCall() =
    methodOrThrow().methodCall()

context(_: BytecodePatchContext)
internal fun MutableMethod.methodCall(): String {
    var methodCall = "$definingClass->$name("
    for (i in 0 until parameters.size) {
        methodCall += parameterTypes[i]
    }
    methodCall += ")$returnType"
    return methodCall
}

/**
 * Modified by lavinhoque33 (2026-10-04) for experimental YouTube 21.39 support:
 * if the flag getter no longer exists (YouTube 21.39 inlines flag getters into their callers),
 * the override is applied at every inlined call site of the flag instead.
 * If the flag literal no longer exists anywhere in the app (A/B test removed), there is
 * nothing to override and a warning is printed instead of failing.
 */
context(context: BytecodePatchContext)
// modified by lavinhoque33, 2026-10-07: returns whether the flag was hooked; optional missing-flag warning
fun Pair<String, Fingerprint>.injectLiteralInstructionBooleanCall(
    literal: Long,
    descriptor: String,
    warnIfMissing: Boolean = true
): Boolean {
    fun MutableMethod.inject(literalIndex: Int) {
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

    val method = second.methodOrNull
    if (method != null) {
        method.inject(method.indexOfFirstLiteralInstruction(literal))
        return true
    }

    if (context.forEachInlinedFeatureFlagSite(literal) { inject(it) } > 0) return true

    var literalExists = false
    context.classDefForEach { classDef ->
        if (!literalExists && classDef.methods.any { it.containsLiteralInstruction(literal) }) {
            literalExists = true
        }
    }
    if (literalExists) throw first.exception

    if (warnIfMissing) {
        printWarn("${first}: feature flag $literal no longer exists in this app version. Skipping.")
    }
    return false
}

context(_: BytecodePatchContext)
fun Pair<String, Fingerprint>.injectLiteralInstructionViewCall(
    literal: Long,
    smaliInstruction: String
) {
    val method = methodOrThrow()
    method.injectLiteralInstructionViewCall(literal, smaliInstruction)
}

// @Deprecated("Migrate away from fingerprint DSL")
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
