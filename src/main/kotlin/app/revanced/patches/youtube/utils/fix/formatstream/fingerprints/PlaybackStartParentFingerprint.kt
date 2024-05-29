package app.revanced.patches.youtube.utils.fix.formatstream.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlaybackStartParentFingerprint : MethodFingerprint(
    returnType = "Lcom/google/android/libraries/youtube/innertube/model/media/VideoStreamingData;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("Invalid playback type; streaming data is not playable"),
)

