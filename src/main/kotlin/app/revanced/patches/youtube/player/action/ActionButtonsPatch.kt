package app.revanced.patches.youtube.player.action

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object ActionButtonsPatch : BaseResourcePatch(
    name = "Hide action buttons",
    description = "Adds options to hide action buttons under videos.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ActionButtonsFilter;"

    override fun execute(context: ResourceContext) {
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_ACTION_BUTTONS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
