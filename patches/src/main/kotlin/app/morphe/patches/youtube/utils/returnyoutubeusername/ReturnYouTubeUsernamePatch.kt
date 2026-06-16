package app.morphe.patches.youtube.utils.returnyoutubeusername

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.returnyoutubeusername.baseReturnYouTubeUsernamePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.patch.PatchList.RETURN_YOUTUBE_USERNAME
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val returnYouTubeUsernamePatch = bytecodePatch(
    RETURN_YOUTUBE_USERNAME.title,
    RETURN_YOUTUBE_USERNAME.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

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
