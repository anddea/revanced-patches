package app.morphe.patches.youtube.player.comments

import app.morphe.patches.youtube.utils.resourceid.emojiPickerIcon
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val shortsLiveStreamEmojiPickerOnClickListenerFingerprint = legacyFingerprint(
    name = "shortsLiveStreamEmojiPickerOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC.value,
    parameters = listOf("L"),
    literals = listOf(126326492L),
)

internal val shortsLiveStreamEmojiPickerOpacityFingerprint = legacyFingerprint(
    name = "shortsLiveStreamEmojiPickerOpacityFingerprint",
    returnType = "Landroid/widget/ImageView;",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(emojiPickerIcon),
)