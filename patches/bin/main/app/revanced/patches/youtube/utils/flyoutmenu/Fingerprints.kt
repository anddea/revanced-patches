package app.revanced.patches.youtube.utils.flyoutmenu

import app.revanced.patches.youtube.utils.resourceid.videoQualityUnavailableAnnouncement
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val videoQualityBottomSheetClassFingerprint = legacyFingerprint(
    name = "videoQualityBottomSheetClassFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    literals = listOf(videoQualityUnavailableAnnouncement),
)
