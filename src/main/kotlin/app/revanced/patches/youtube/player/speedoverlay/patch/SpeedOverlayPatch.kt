package app.revanced.patches.youtube.player.speedoverlay.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayConfigFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayHookFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.YouTubeTextViewFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.SpeedOverlayText
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("Custom speed overlay")
@Description("Customize 'Play at 2x speed' while holding down.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class SpeedOverlayPatch : BytecodePatch(
    listOf(
        SpeedOverlayConfigFingerprint,
        SpeedOverlayHookFingerprint,
        YouTubeTextViewFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        SpeedOverlayConfigFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->disableSpeedOverlay(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw SpeedOverlayConfigFingerprint.exception

        SpeedOverlayHookFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.CMPL_FLOAT
                } + 3
                val insertRegister = getInstruction<Instruction35c>(insertIndex).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->getSpeed(F)F
                        move-result v$insertRegister
                        """
                )
            }
        } ?: throw SpeedOverlayHookFingerprint.exception

        YouTubeTextViewFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetInstruction = getInstruction<Instruction35c>(targetIndex)
                val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference

                addInstructions(
                    targetIndex + 1, """
                        const v0, $SpeedOverlayText
                        invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}, v0}, $INTEGRATIONS_CLASS_DESCRIPTOR->getSpeedText(Landroid/widget/TextView;Ljava/lang/CharSequence;I)Ljava/lang/CharSequence;
                        move-result-object v${targetInstruction.registerD}
                        invoke-super {v${targetInstruction.registerC}, v${targetInstruction.registerD}, v${targetInstruction.registerE}}, $targetReference
                        """
                )
                removeInstruction(targetIndex)
            }
        } ?: throw YouTubeTextViewFingerprint.exception

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

    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$UTILS_PATH/SpeedOverlayPatch;"
    }
}
