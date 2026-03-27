package app.morphe.patches.youtube.general.autocaptions

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.captions.baseAutoCaptionsPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_FORCED_AUTO_CAPTIONS
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val autoCaptionsPatch = bytecodePatch(
    DISABLE_FORCED_AUTO_CAPTIONS.title,
    DISABLE_FORCED_AUTO_CAPTIONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseAutoCaptionsPatch,
        settingsPatch,
    )

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_AUTO_CAPTIONS"
            ),
            DISABLE_FORCED_AUTO_CAPTIONS
        )

        // endregion

    }
}
