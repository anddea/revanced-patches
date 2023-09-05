package app.revanced.patches.music.buttoncontainer.radio.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.buttoncontainerhook.patch.ButtonContainerHookPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType

@Patch
@Name("Hide radio button")
@Description("Hides start radio button.")
@DependsOn(
    [
        ButtonContainerHookPatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
class HideRadioButtonPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.BUTTON_CONTAINER,
            "revanced_hide_button_container_radio",
            "false"
        )

    }
}
