package app.revanced.patches.youtube.misc.ambientmode

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeAlternativeFingerprint
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Bypass ambient mode restrictions",
    description = "Bypass ambient mode restrictions in battery saver mode.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
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
object PowerSaveModePatch : BytecodePatch(
    setOf(
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
