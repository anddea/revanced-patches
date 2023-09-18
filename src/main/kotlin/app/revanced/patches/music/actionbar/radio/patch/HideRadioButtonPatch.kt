package app.revanced.patches.music.actionbar.radio.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.actionbarhook.patch.ActionBarHookPatch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType

@Patch
@Name("Hide radio button")
@Description("Hides start radio button.")
@DependsOn(
    [
        ActionBarHookPatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
class HideRadioButtonPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_bar_radio",
            "false"
        )

    }
}
