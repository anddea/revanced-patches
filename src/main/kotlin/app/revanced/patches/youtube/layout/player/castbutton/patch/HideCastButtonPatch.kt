package app.revanced.patches.youtube.layout.player.castbutton.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER_LAYOUT

@Patch
@Name("hide-cast-button")
@Description("Hides the cast button in the video player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HideCastButtonPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (classDef.type.endsWith("MediaRouteButton;") && method.name == "setVisibility") {
                    val setVisibilityMethod =
                        context.proxy(classDef).mutableClass.methods.first { it.name == "setVisibility" }

                    setVisibilityMethod.addInstructions(
                        0, """
                            invoke-static {p1}, $PLAYER_LAYOUT->hideCastButton(I)I
                            move-result p1
                        """
                    )
                }
            }
        }

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: LAYOUT_SETTINGS",
                "PREFERENCE_HEADER: PLAYER",
                "SETTINGS: HIDE_CAST_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-cast-button")

        return PatchResultSuccess()
    }
}
