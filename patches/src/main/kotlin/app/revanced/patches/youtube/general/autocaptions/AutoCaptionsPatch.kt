package app.revanced.patches.youtube.general.autocaptions

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.captions.baseAutoCaptionsPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_AUTO_CAPTIONS
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val autoCaptionsPatch = bytecodePatch(
    DISABLE_AUTO_CAPTIONS.title,
    DISABLE_AUTO_CAPTIONS.summary,
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
            DISABLE_AUTO_CAPTIONS
        )

        // endregion

    }
}
