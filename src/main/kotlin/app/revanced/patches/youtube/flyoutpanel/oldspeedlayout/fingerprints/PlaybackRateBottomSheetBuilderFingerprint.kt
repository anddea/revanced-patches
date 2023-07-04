package app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object PlaybackRateBottomSheetBuilderFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    strings = listOf("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT")
)