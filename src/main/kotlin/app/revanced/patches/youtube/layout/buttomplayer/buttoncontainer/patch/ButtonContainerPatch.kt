package app.revanced.patches.youtube.layout.buttomplayer.buttoncontainer.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.litho.patch.ByteBufferFilterPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.BOTTOM_PLAYER

@Patch
@Name("hide-button-container")
@Description("Adds the options to hide action buttons under a video.")
@DependsOn(
    [
        ByteBufferFilterPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ButtonContainerPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        ByteBufferFilterPatch.inject("$BOTTOM_PLAYER->hideActionButtons")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "SETTINGS: BUTTON_CONTAINER"
            )
        )

        SettingsPatch.updatePatchStatus("hide-button-container")

        return PatchResultSuccess()
    }
}
