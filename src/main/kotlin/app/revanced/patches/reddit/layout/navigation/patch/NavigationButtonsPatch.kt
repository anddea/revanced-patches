package app.revanced.patches.reddit.layout.navigation.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenFingerprint
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.resource.patch.SettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch
@Name("Hide navigation buttons")
@Description("Hide buttons at navigation bar.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Version("0.0.1")
class NavigationButtonsPatch : BytecodePatch(
    listOf(BottomNavScreenFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        BottomNavScreenFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister =
                    getInstruction<FiveRegisterInstruction>(startIndex).registerC

                addInstruction(
                    startIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR"
                )
            }
        } ?: return BottomNavScreenFingerprint.toErrorResult()

        updateSettingsStatus("NavigationButtons")

        return PatchResultSuccess()
    }

    companion object {
        const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/NavigationButtonsPatch;" +
                    "->hideNavigationButtons(Landroid/view/ViewGroup;)V"
    }
}
