package app.revanced.patches.youtube.layout.general.floatingmicrophone.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.floatingmicrophone.fingerprints.FloatingMicrophoneFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-floating-microphone")
@Description("Hide the floating microphone button above the keyboard.")
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
                val register = instruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$register}, $GENERAL->hideFloatingMicrophone(Z)Z
                        move-result v$register
                        """
                )
            }
        } ?: return FloatingMicrophoneFingerprint.toErrorResult()

        /*
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
