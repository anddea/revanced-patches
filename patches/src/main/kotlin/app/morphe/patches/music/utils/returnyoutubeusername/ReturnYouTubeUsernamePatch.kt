package app.morphe.patches.music.utils.returnyoutubeusername

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.patch.PatchList.RETURN_YOUTUBE_USERNAME
import app.morphe.patches.music.utils.playservice.is_6_42_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addPreferenceWithIntent
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.returnyoutubeusername.baseReturnYouTubeUsernamePatch

@Suppress("unused")
val returnYouTubeUsernamePatch = resourcePatch(
    RETURN_YOUTUBE_USERNAME.title,
    RETURN_YOUTUBE_USERNAME.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseReturnYouTubeUsernamePatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_USERNAME,
            "revanced_return_youtube_username_enabled",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.RETURN_YOUTUBE_USERNAME,
            "revanced_return_youtube_username_display_format",
            "revanced_return_youtube_username_enabled"
        )
        addPreferenceWithIntent(
            CategoryType.RETURN_YOUTUBE_USERNAME,
            "revanced_return_youtube_username_youtube_data_api_v3_developer_key",
            "revanced_return_youtube_username_enabled"
        )
        if (is_6_42_or_greater) {
            addPreferenceWithIntent(
                CategoryType.RETURN_YOUTUBE_USERNAME,
                "revanced_return_youtube_username_youtube_data_api_v3_about"
            )
        }

        updatePatchStatus(RETURN_YOUTUBE_USERNAME)

    }
}