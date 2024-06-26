package app.revanced.patches.youtube.utils.fix.parameter

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.storyboard.StoryboardHookPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.patches.youtube.video.playerresponse.PlayerResponseMethodHookPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
@Deprecated("This patch will be removed in the future.")
object SpoofPlayerParameterPatch : BaseBytecodePatch(
    // name = "Spoof player parameters",
    description = "Adds options to spoof player parameters to prevent playback issues.",
    dependencies = setOf(
        PlayerTypeHookPatch::class,
        PlayerResponseMethodHookPatch::class,
        SettingsPatch::class,
        VideoInformationPatch::class,
        StoryboardHookPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/SpoofPlayerParameterPatch;"

    override fun execute(context: BytecodeContext) {

        // Hook the player parameters.
        PlayerResponseMethodHookPatch += PlayerResponseMethodHookPatch.Hook.PlayerParameter(
            "$INTEGRATIONS_CLASS_DESCRIPTOR->spoofParameter(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;"
        )

        // Hook storyboard.
        StoryboardHookPatch.hook(INTEGRATIONS_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_PLAYER_PARAMETER"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
