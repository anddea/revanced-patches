package app.revanced.patches.youtube.layout.general.pivotbar.switchbutton.patch

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
import app.revanced.patches.youtube.layout.general.pivotbar.switchbutton.fingerprints.AutoMotiveFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("switch-create-notification")
@Description("Switching the create button and notification button.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class SwitchCreateButtonPatch : BytecodePatch(
    listOf(AutoMotiveFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        AutoMotiveFingerprint.result?.let {
            with (it.mutableMethod) {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = (instruction(insertIndex) as OneRegisterInstruction).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$register}, $GENERAL_LAYOUT->switchCreateNotification(Z)Z
                        move-result v$register
                        """
                )
            }
        } ?: return AutoMotiveFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_LAYOUT_SETTINGS",
                "SETTINGS: SWITCH_CREATE_NOTIFICATION"
            )
        )

        SettingsPatch.updatePatchStatus("switch-create-notification")

        return PatchResultSuccess()
    }
}