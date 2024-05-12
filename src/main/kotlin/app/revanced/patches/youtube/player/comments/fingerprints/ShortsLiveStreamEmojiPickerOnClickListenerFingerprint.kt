package app.revanced.patches.youtube.player.comments.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ShortsLiveStreamEmojiPickerOnClickListenerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    accessFlags = AccessFlags.PUBLIC.value,
    literalSupplier = { 126326492 }
)