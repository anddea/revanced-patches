package app.revanced.patches.youtube.player.speedoverlay.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayConfigFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide speed overlay")
@Description("Hide speed overlay in player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class HideSpeedOverlayPatch : BytecodePatch(
    listOf(SpeedOverlayConfigFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        SpeedOverlayConfigFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$targetRegister}, $PLAYER->hideSpeedOverlay(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw SpeedOverlayConfigFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_SPEED_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("hide-speed-overlay")

    }
}
