package app.revanced.patches.youtube.misc.ambientmode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("bypass-ambient-mode-restrictions")
@Description("Bypass ambient mode restrictions in battery saver mode.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class PowerSaveModePatch : BytecodePatch(
    listOf(PowerSaveModeFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PowerSaveModeFingerprint.result?.mutableMethod?.let { method ->
            val instructions = method.implementation!!.instructions
            var powerManagerIndex = -1

            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) continue

                val invokeInstruction = instruction as Instruction35c
                if ((invokeInstruction.reference as MethodReference).name != "isPowerSaveMode") continue

                powerManagerIndex = index + 1

                val targetRegister = (instructions.elementAt(powerManagerIndex) as OneRegisterInstruction).registerA

                method.addInstructions(
                    powerManagerIndex + 1, """
                        invoke-static {v$targetRegister}, $MISC_PATH/PowerSaveModePatch;->bypassPowerSaveModeRestrictions(Z)Z
                        move-result v$targetRegister
                    """
                )
            }
            if (powerManagerIndex == -1) return PatchResultError("Couldn't find PowerManager reference")
        } ?: return PowerSaveModeFingerprint.toErrorResult()

        /*
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
