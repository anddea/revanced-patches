package app.revanced.patches.youtube.player.speedoverlay

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayConfigFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayHookFingerprint
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.YouTubeTextViewFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.SpeedOverlayText
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch(
    name = "Custom speed overlay",
    description = "Customize 'Play at 2x speed' while holding down.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39"
            ]
        )
    ]
)
@Suppress("unused")
object SpeedOverlayPatch : BytecodePatch(
    setOf(
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

    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$UTILS_PATH/SpeedOverlayPatch;"
}
