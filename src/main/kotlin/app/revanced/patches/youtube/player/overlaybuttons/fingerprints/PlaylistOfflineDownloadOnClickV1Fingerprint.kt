package app.revanced.patches.youtube.player.overlaybuttons.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ActionsBar
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ActionsBarV2
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object PlaylistOfflineDownloadOnClickV1Fingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literalSupplier = { ActionsBar },
)