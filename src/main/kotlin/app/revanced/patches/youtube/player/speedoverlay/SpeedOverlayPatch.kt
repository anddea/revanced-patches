package app.revanced.patches.youtube.player.speedoverlay

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.player.speedoverlay.fingerprints.SpeedOverlayFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(
    name = "Disable speed overlay",
    description = "Disable 'Play at 2x speed' while holding down.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34"
            ]
        )
    ]
)
@Suppress("unused")
object SpeedOverlayPatch : BytecodePatch(
    setOf(SpeedOverlayFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        SpeedOverlayFingerprint.result?.let {
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex

                for (index in endIndex downTo 0) {
                    if (getInstruction(index).opcode != Opcode.RETURN_VOID) continue

                    val replaceIndex = index + 1

                    val replaceReference =
                        getInstruction<ReferenceInstruction>(replaceIndex).reference

                    val replaceInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
                    val registerA = replaceInstruction.registerA
                    val registerB = replaceInstruction.registerB

                    addInstructionsWithLabels(
                        replaceIndex + 1, """
                            invoke-static { }, $PLAYER->disableSpeedOverlay()Z
                            move-result v$registerA
                            if-eqz v$registerA, :show
                            return-void
                            :show
                            iget-object v$registerA, v$registerB, $replaceReference
                            """
                    )
                    removeInstruction(replaceIndex)

                    break
                }
            }
        } ?: throw SpeedOverlayFingerprint.exception


        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: DISABLE_SPEED_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("Disable speed overlay")

    }
}