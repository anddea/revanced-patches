package app.revanced.patches.music.actionbar.radio

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.actionbarhook.ActionBarHookPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType

@Patch(
    name = "Hide radio button",
    description = "Hides start radio button.",
    dependencies = [
        ActionBarHookPatch::class,
        SettingsPatch::class
    ],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object HideRadioButtonPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_bar_radio",
            "false"
        )

    }
}
