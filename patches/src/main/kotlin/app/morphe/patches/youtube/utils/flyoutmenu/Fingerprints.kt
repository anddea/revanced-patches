package app.morphe.patches.youtube.utils.flyoutmenu

import app.morphe.patches.youtube.utils.resourceid.videoQualityUnavailableAnnouncement
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val videoQualityBottomSheetClassFingerprint = legacyFingerprint(
    name = "videoQualityBottomSheetClassFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    literals = listOf(videoQualityUnavailableAnnouncement),
)
