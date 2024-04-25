package app.revanced.patches.youtube.misc.ambientmode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.AmbientModeInFullscreenFingerprint
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeFingerprint
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeOneFingerprint
import app.revanced.patches.youtube.misc.ambientmode.fingerprints.PowerSaveModeTwoFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.FULLSCREEN
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Ambient mode switch",
    description = "Adds an option to bypass the restrictions of ambient mode or disable it completely.",
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
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43",
                "19.15.36",
                "19.16.38"
            ]
        )
    ]
)
@Suppress("unused")
object AmbientModeSwitchPatch : BytecodePatch(
    setOf(
        AmbientModeInFullscreenFingerprint,
        PowerSaveModeFingerprint,
        PowerSaveModeOneFingerprint,
        PowerSaveModeTwoFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

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
                            invoke-static {v$targetRegister}, $MISC_PATH/AmbientModePatch;->bypassPowerSaveModeRestrictions(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
                if (insertIndex == -1)
                    throw PatchException("Couldn't find PowerManager reference")
            }
        } ?: arrayOf(
            PowerSaveModeOneFingerprint,
            PowerSaveModeTwoFingerprint
        ).forEach { fingerprint ->
            fingerprint.result?.let {
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
                                invoke-static {v$targetRegister}, $MISC_PATH/AmbientModePatch;->bypassPowerSaveModeRestrictions(Z)Z
                                move-result v$targetRegister
                                """
                        )
                    }
                if (insertIndex == -1)
                    throw PatchException("Couldn't find PowerManager reference")
                }
            } ?: throw PowerSaveModeFingerprint.exception
        }

        AmbientModeInFullscreenFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(45389368) + 3
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {}, $FULLSCREEN->disableAmbientMode()Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw AmbientModeInFullscreenFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: AMBIENT_MODE_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("Ambient mode switch")

    }
}