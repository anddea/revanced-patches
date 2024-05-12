package app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

/**
 * This fingerprint is compatible with all YouTube versions after v18.15.40.
 */
internal object UserWasInShortsABConfigFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL
    ),
    strings = listOf("Failed to get offline response: ")
)