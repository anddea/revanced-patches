package app.morphe.patches.youtube.utils.fix.hype

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.spoof.watchnext.spoofAppVersionWatchNextPatch
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.playservice.is_19_26_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources

private val hypeButtonIconResourcePatch = resourcePatch(
    description = "hypeButtonIconResourcePatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (is_19_26_or_greater) {
            return@execute
        }
        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).forEach { dpi ->
            copyResources(
                "youtube/hype",
                ResourceGroup(
                    "drawable-$dpi",
                    "yt_fill_star_shooting_black_24.png",
                    "yt_outline_star_shooting_black_24.png"
                )
            )
        }
    }
}

/**
 * 1. YouTube 19.25.39 can be used without the 'Disable update screen' patch.
 *    This means that even if you use an unpatched YouTube 19.25.39, the 'Update your app' pop-up will not appear.
 * 2. Due to a server-side change, the Hype button is now available on YouTube 19.25.39 and earlier.
 * 3. Google did not add the Hype icon (R.drawable.yt_outline_star_shooting_black_24) to YouTube 19.25.39 or earlier,
 *    So no icon appears on the Hype button when using YouTube 19.25.39.
 * 4. For the same reason, the 'buttonViewModel.iconName' value in the '/next' endpoint response from YouTube 19.25.39 is also empty.
 * 5. There was an issue where the feed layout would break if the app version was spoofed to 19.26.42 via the 'Spoof app version' patch.
 * 6. As a workaround, the app version is spoofed to 19.26.42 only for the '/get_watch' and '/next' endpoints.
 */
val hypeButtonIconPatch = spoofAppVersionWatchNextPatch(
    block = {
        dependsOn(
            settingsPatch,
            hypeButtonIconResourcePatch,
            versionCheckPatch
        )
    },
    patchRequired = {
        !is_19_26_or_greater
    },
    availabilityDescriptor = "$GENERAL_CLASS_DESCRIPTOR->fixHypeButtonIconEnabled()Z",
    appVersionDescriptor = "$GENERAL_CLASS_DESCRIPTOR->getWatchNextEndpointVersionOverride()Ljava/lang/String;",
    executeBlock = {
        if (!is_19_26_or_greater) {
            addPreference(
                arrayOf(
                    "PREFERENCE_SCREEN: GENERAL",
                    "SETTINGS: FIX_HYPE_BUTTON_ICON"
                )
            )
        }
    }
)
