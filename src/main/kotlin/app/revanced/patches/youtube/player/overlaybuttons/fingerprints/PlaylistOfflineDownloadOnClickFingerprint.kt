package app.revanced.patches.youtube.player.overlaybuttons.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ActionsBarV2
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// TODO: why the literalSupplier is ignored
object PlaylistOfflineDownloadOnClickFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literalSupplier = { ActionsBarV2 },
)