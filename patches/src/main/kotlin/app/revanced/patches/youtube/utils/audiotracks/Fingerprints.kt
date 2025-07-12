package app.revanced.patches.youtube.utils.audiotracks

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val audioStreamingTypeSelector = legacyFingerprint(
    name = "audioStreamingTypeSelector",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "L",
    strings = listOf("raw")
)

internal val menuItemAudioTrackFingerprint = legacyFingerprint(
    name = "menuItemAudioTrackFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    returnType = "V",
    strings = listOf("menu_item_audio_track")
)

internal val streamingModelBuilderFingerprint = legacyFingerprint(
    name = "streamingModelBuilderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    strings = listOf("vprng")
)
