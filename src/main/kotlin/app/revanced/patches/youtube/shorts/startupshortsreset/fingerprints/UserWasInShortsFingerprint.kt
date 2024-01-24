package app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object UserWasInShortsFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    opcodes = listOf(Opcode.CONST_WIDE_32),
    strings = listOf("Failed to read user_was_in_shorts proto after successful warmup")
)