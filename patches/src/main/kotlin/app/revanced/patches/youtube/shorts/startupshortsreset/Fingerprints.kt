package app.revanced.patches.youtube.shorts.startupshortsreset

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * YouTube v18.15.40 ~ YouTube 19.46.42
 */
internal val userWasInShortsABConfigFingerprint = legacyFingerprint(
    name = "userWasInShortsABConfigFingerprint",
    returnType = "V",
    strings = listOf("Failed to get offline response: "),
    customFingerprint = { method, _ ->
        indexOfOptionalInstruction(method) >= 0
    }
)

internal fun indexOfOptionalInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_STATIC &&
                getReference<MethodReference>().toString() == "Lj${'$'}/util/Optional;->of(Ljava/lang/Object;)Lj${'$'}/util/Optional;"
    }

/**
 * YouTube 19.47.53 ~
 */
internal val userWasInShortsABConfigAlternativeFingerprint = legacyFingerprint(
    name = "userWasInShortsABConfigAlternativeFingerprint",
    returnType = "V",
    parameters = listOf("I"),
    opcodes = listOf(Opcode.OR_INT_LIT8),
    strings = listOf("alias", "null"),
)

/**
 * ~ YouTube 19.50.42
 */
internal val userWasInShortsFingerprint = legacyFingerprint(
    name = "userWasInShortsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("Failed to read user_was_in_shorts proto after successful warmup")
)

/**
 * YouTube 20.02.08 ~
 */
internal val userWasInShortsAlternativeFingerprint = legacyFingerprint(
    name = "userWasInShortsAlternativeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("userIsInShorts: ")
)