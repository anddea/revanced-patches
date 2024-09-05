package app.revanced.patches.youtube.general.downloads.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AccessibilityOfflineButtonSync
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object AccessibilityOfflineButtonSyncFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    literalSupplier = { AccessibilityOfflineButtonSync }
)