package app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object UserWasInShortsFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("Failed to read user_was_in_shorts proto after successful warmup"),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45381394) }
)