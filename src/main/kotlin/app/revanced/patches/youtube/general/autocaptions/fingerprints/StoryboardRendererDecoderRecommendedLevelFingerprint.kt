package app.revanced.patches.youtube.general.autocaptions.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.PlayerResponseModelUtils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import com.android.tools.smali.dexlib2.AccessFlags

internal object StoryboardRendererDecoderRecommendedLevelFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR),
    strings = listOf("#-1#")
)
