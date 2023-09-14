package app.revanced.patches.youtube.misc.ambientmode.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeAlternativeFingerprint
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch
@Name("Bypass ambient mode restrictions")
@Description("Bypass ambient mode restrictions in battery saver mode.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class PowerSaveModePatch : BytecodePatch(
    listOf(
        PowerSaveModeAlternativeFingerprint,
        PowerSaveModeFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        val result =
            PowerSaveModeFingerprint.result
                ?: PowerSaveModeAlternativeFingerprint.result
                ?: throw PowerSaveModeFingerprint.exception

        result.let {
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
                    throw PatchException("Couldn't find PowerManager reference")
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: BYPASS_AMBIENT_MODE_RESTRICTIONS"
            )
        )

        SettingsPatch.updatePatchStatus("bypass-ambient-mode-restrictions")

    }
}
