package app.revanced.patches.youtube.utils.returnyoutubeusername

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.returnyoutubeusername.baseReturnYouTubeUsernamePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.RETURN_YOUTUBE_USERNAME
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val returnYouTubeUsernamePatch = bytecodePatch(
    RETURN_YOUTUBE_USERNAME.title,
    RETURN_YOUTUBE_USERNAME.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseReturnYouTubeUsernamePatch,
        settingsPatch,
    )

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: RETURN_YOUTUBE_USERNAME"
            ),
            RETURN_YOUTUBE_USERNAME
        )

        // endregion
    }
}
