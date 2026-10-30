package app.morphe.patches.youtube.general.autocaptions

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.patch.PatchList.CAPTIONS
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val captionsPatch = bytecodePatch(
    name = "Captions",
    description = "Adds options to disable captions from being automatically enabled, " +
            "select a preferred and a secondary caption language, add more auto-translate languages, " +
            "and show captions in stable blocks.",
) {
    dependsOn(
        settingsPatch,
        autoCaptionsPatch,
        preferredCaptionLanguagePatch,
        translatedCaptionPensPatch,
        captionTrackCapturePatch,
        captionWindowMeasurePatch,
        stableCaptionBlocksPatch,
        captionViewportPatch,
    )
    compatibleWith(COMPATIBILITY_YOUTUBE)
    execute {
        addPreference(arrayOf("PREFERENCE_SCREEN: PLAYER", "SETTINGS: BILINGUAL_CAPTIONS"), CAPTIONS)
    }
}
