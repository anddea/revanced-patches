package app.revanced.patches.youtube.utils.fix.litho.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * This fingerprint is compatible with YouTube v19.19.39+
 */
internal object ObfuscationConfigFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literalSupplier = { 45631264 }
)