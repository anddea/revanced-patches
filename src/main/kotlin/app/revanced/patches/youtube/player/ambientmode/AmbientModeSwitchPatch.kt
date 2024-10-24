package app.revanced.patches.youtube.player.ambientmode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.player.ambientmode.fingerprints.AmbientModeInFullscreenFingerprint
import app.revanced.patches.youtube.player.ambientmode.fingerprints.PowerSaveModeBroadcastReceiverFingerprint
import app.revanced.patches.youtube.player.ambientmode.fingerprints.PowerSaveModeSyntheticFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.findMethodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.injectLiteralInstructionBooleanCall
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object AmbientModeSwitchPatch : BaseBytecodePatch(
    name = "Ambient mode control",
    description = "Adds options to disable Ambient mode and to bypass Ambient mode restrictions.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AmbientModeInFullscreenFingerprint,
        PowerSaveModeBroadcastReceiverFingerprint,
        PowerSaveModeSyntheticFingerprint
    )
) {
    private var syntheticClassList = emptyArray<String>()

    override fun execute(context: BytecodeContext) {

        // region patch for bypass ambient mode restrictions

        mapOf(
            PowerSaveModeBroadcastReceiverFingerprint to false,
            PowerSaveModeSyntheticFingerprint to true
        ).forEach { (fingerprint, reversed) ->
            fingerprint.resultOrThrow().mutableMethod.apply {
                val stringIndex =
                    indexOfFirstStringInstructionOrThrow("android.os.action.POWER_SAVE_MODE_CHANGED")
                val targetIndex =
                    if (reversed)
                        indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.INVOKE_DIRECT)
                    else
                        indexOfFirstInstructionOrThrow(stringIndex, Opcode.INVOKE_DIRECT)
                val targetClass =
                    (getInstruction<ReferenceInstruction>(targetIndex).reference as MethodReference).definingClass

                syntheticClassList += targetClass
            }
        }

        syntheticClassList.distinct().forEach { className ->
            context.findMethodOrThrow(className) {
                name == "accept"
            }.apply {
                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        val reference = (instruction as? ReferenceInstruction)?.reference
                        instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                                reference is MethodReference &&
                                reference.name == "isPowerSaveMode"
                    }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                        addInstructions(
                            index + 2, """
                                invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->bypassAmbientModeRestrictions(Z)Z
                                move-result v$register
                                """
                        )
                    }
            }
        }

        // endregion

        // region patch for disable ambient mode in fullscreen

        AmbientModeInFullscreenFingerprint.injectLiteralInstructionBooleanCall(
            45389368,
            "$PLAYER_CLASS_DESCRIPTOR->disableAmbientModeInFullscreen()Z"
        )

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: AMBIENT_MODE_CONTROLS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}