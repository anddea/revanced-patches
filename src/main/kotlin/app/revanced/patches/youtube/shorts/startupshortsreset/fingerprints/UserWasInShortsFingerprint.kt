package app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object UserWasInShortsFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("Failed to read user_was_in_shorts proto after successful warmup"),
    literalSupplier = { 45381394 }
)