package app.revanced.patches.reddit.layout.toolbar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.reddit.layout.toolbar.fingerprints.HomePagerScreenFingerprint
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch.ToolBarNavSearchCtaContainer
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
@Deprecated("This patch is deprecated until Reddit adds a button like r/place or Reddit recap button to the toolbar.")
object ToolBarButtonPatch : BaseBytecodePatch(
    // name = "Hide toolbar button",
    description = "Adds an option to hide the r/place or Reddit recap button in the toolbar.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(HomePagerScreenFingerprint)
) {
    private const val INTEGRATIONS_METHOD_DESCRIPTOR =
        "$PATCHES_PATH/ToolBarButtonPatch;->hideToolBarButton(Landroid/view/View;)V"

    override fun execute(context: BytecodeContext) {

        HomePagerScreenFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex =
                    getWideLiteralInstructionIndex(ToolBarNavSearchCtaContainer) + 3
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(targetIndex - 1).registerA

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR"
                )
            }
        }

        updateSettingsStatus("enableToolBarButton")

    }
}
