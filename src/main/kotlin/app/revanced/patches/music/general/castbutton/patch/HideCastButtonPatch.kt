package app.revanced.patches.music.general.castbutton.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.general.castbutton.fingerprints.MediaRouteButtonFingerprint
import app.revanced.patches.music.general.castbutton.fingerprints.PlayerOverlayChipFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.PlayerOverlayChip
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide cast button")
@Description("Hides the cast button.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class HideCastButtonPatch : BytecodePatch(
    listOf(
        MediaRouteButtonFingerprint,
        PlayerOverlayChipFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hide cast button
         */
        MediaRouteButtonFingerprint.result?.let {
            val setVisibilityMethod = it.mutableClass.methods.find { method -> method.name == "setVisibility" }

            setVisibilityMethod?.apply {
                addInstructions(
                    0, """
                        invoke-static {p1}, $MUSIC_GENERAL->hideCastButton(I)I
                        move-result p1
                        """
                )
            } ?: throw PatchException("Failed to find setVisibility method")
        } ?: throw MediaRouteButtonFingerprint.exception

        /**
         * Hide floating cast banner
         */
        PlayerOverlayChipFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(PlayerOverlayChip) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $MUSIC_GENERAL->hideCastButton(Landroid/view/View;)V"
                )
            }
        }

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_hide_cast_button",
            "true"
        )

    }
}
