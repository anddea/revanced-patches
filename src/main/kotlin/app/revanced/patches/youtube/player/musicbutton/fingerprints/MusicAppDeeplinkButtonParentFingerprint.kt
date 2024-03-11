package app.revanced.patches.youtube.player.musicbutton.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.MusicAppDeeplinkButtonView
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object MusicAppDeeplinkButtonParentFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    literalSupplier = { MusicAppDeeplinkButtonView }
)