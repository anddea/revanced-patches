package app.revanced.patches.youtube.general.floatingmicrophone.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.general.floatingmicrophone.fingerprints.FloatingMicrophoneFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Hide floating microphone")
@Description("Hides the floating microphone button which appears in search.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class FloatingMicrophonePatch : BytecodePatch(
    listOf(FloatingMicrophoneFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        FloatingMicrophoneFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$register}, $GENERAL->hideFloatingMicrophone(Z)Z
                        move-result v$register
                        """
                )
            }
        } ?: return FloatingMicrophoneFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_FLOATING_MICROPHONE"
            )
        )

        SettingsPatch.updatePatchStatus("hide-floating-microphone")

        return PatchResultSuccess()
    }
}
