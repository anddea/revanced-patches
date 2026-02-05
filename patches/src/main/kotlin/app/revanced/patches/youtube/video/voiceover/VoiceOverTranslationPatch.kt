package app.revanced.patches.youtube.video.voiceover

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.spoof.streamingdata.spoofStreamingDataPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.VOICE_OVER_TRANSLATION
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val voiceOverTranslationPatch = bytecodePatch(
    VOICE_OVER_TRANSLATION.title,
    VOICE_OVER_TRANSLATION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        spoofStreamingDataPatch(),
    )

    execute {
        // The actual implementation is in VoiceOverTranslationPatch.java
        // which is called from SpoofStreamingDataPatch.addVoiceOverTranslation()
        
        // Add settings preference
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: VIDEO"
            ),
            VOICE_OVER_TRANSLATION
        )
    }
}
