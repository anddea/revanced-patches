package app.revanced.patches.reddit.layout.navigation

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenFingerprint
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
object NavigationButtonsPatch : BaseBytecodePatch(
    name = "Hide navigation buttons",
    description = "Adds options to hide buttons in the navigation bar.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(BottomNavScreenFingerprint)
) {
    private const val INTEGRATIONS_METHOD_DESCRIPTOR =
        "$PATCHES_PATH/NavigationButtonsPatch;->hideNavigationButtons(Landroid/view/ViewGroup;)V"

    override fun execute(context: BytecodeContext) {

        BottomNavScreenFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister =
                    getInstruction<FiveRegisterInstruction>(startIndex).registerC

                addInstruction(
                    startIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR"
                )
            }
        }

        updateSettingsStatus("enableNavigationButtons")

    }
}
