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
 * This fingerprint is compatible with all YouTube versions after v18.15.40.
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

internal val userWasInShortsFingerprint = legacyFingerprint(
    name = "userWasInShortsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("Failed to read user_was_in_shorts proto after successful warmup")
)