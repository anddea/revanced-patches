package app.revanced.patches.youtube.misc.ambientmode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch
@Name("Bypass ambient mode restrictions")
@Description("Bypass ambient mode restrictions in battery saver mode.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class PowerSaveModePatch : BytecodePatch(
    listOf(PowerSaveModeFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PowerSaveModeFingerprint.result?.let {
            it.mutableMethod.apply {
                var insertIndex = -1

                for ((index, instruction) in implementation!!.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) continue

                    val invokeInstruction = instruction as Instruction35c
                    if ((invokeInstruction.reference as MethodReference).name != "isPowerSaveMode") continue

                    val targetRegister = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    insertIndex = index + 2

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$targetRegister}, $MISC_PATH/PowerSaveModePatch;->bypassPowerSaveModeRestrictions(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
                if (insertIndex == -1)
                    return PatchResultError("Couldn't find PowerManager reference")
            }
        } ?: return PowerSaveModeFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: BYPASS_AMBIENT_MODE_RESTRICTIONS"
            )
        )

        SettingsPatch.updatePatchStatus("bypass-ambient-mode-restrictions")

        return PatchResultSuccess()
    }
}
