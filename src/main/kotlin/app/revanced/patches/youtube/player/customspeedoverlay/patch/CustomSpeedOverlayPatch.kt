package app.revanced.patches.youtube.player.customspeedoverlay.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.player.customspeedoverlay.fingerprints.SpeedOverlayHookFingerprint
import app.revanced.patches.youtube.player.customspeedoverlay.fingerprints.YouTubeTextViewFingerprint
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("custom-speed-overlay")
@Description("Customize the video speed that changes when pressing and holding the player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class CustomSpeedOverlayPatch : BytecodePatch(
    listOf(
        SpeedOverlayHookFingerprint,
        YouTubeTextViewFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SpeedOverlayHookFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<Instruction35c>(insertIndex).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER->customSpeedOverlay(F)F
                        move-result v$insertRegister
                        """
                )
            }
        } ?: return SpeedOverlayHookFingerprint.toErrorResult()

        YouTubeTextViewFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetInstruction = getInstruction<Instruction35c>(targetIndex)
                val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference

                replaceInstruction(
                    targetIndex,
                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, $PLAYER->customSpeedOverlay(Landroid/widget/TextView;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;"
                )
                addInstructions(
                    targetIndex + 1, """
                        move-result-object v${targetInstruction.registerD}
                        invoke-super {v${targetInstruction.registerC}, v${targetInstruction.registerD}, v${targetInstruction.registerE}}, $targetReference
                        """
                )
            }
        } ?: return YouTubeTextViewFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: CUSTOM_SPEED_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("custom-speed-overlay")

        return PatchResultSuccess()
    }
}
