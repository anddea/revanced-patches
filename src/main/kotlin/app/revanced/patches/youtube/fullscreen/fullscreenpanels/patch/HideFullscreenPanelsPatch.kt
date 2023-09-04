package app.revanced.patches.youtube.fullscreen.fullscreenpanels.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.fullscreen.fullscreenpanels.fingerprints.FullscreenEngagementPanelFingerprint
import app.revanced.patches.youtube.fullscreen.fullscreenpanels.fingerprints.FullscreenViewAdderFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.utils.quickactions.patch.QuickActionsHookPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.FullScreenEngagementPanel
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getNarrowLiteralIndex
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.FULLSCREEN
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("Hide fullscreen panels")
@Description("Hides video description and comments panel in fullscreen view.")
@DependsOn(
    [
        QuickActionsHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class HideFullscreenPanelsPatch : BytecodePatch(
    listOf(
        FullscreenEngagementPanelFingerprint,
        FullscreenViewAdderFingerprint,
        LayoutConstructorFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        FullscreenEngagementPanelFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(FullScreenEngagementPanel) + 3
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FULLSCREEN->hideFullscreenPanels(Landroidx/coordinatorlayout/widget/CoordinatorLayout;)V"
                )
            }
        } ?: throw FullscreenEngagementPanelFingerprint.exception

        FullscreenViewAdderFingerprint.result?.let {
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<Instruction35c>(endIndex).registerD

                for (i in 1..3) removeInstruction(endIndex - i)

                addInstructions(
                    endIndex - 3, """
                        invoke-static {}, $FULLSCREEN->hideFullscreenPanels()I
                        move-result v$register
                    """
                )
            }
        }

        LayoutConstructorFingerprint.result?.let {
            it.mutableMethod.apply {
                val dummyRegister =
                    getInstruction<OneRegisterInstruction>(getNarrowLiteralIndex(159962)).registerA

                val invokeIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                            ((instruction as ReferenceInstruction).reference.toString() ==
                                    "Landroid/widget/FrameLayout;->addView(Landroid/view/View;)V")
                }

                addInstructionsWithLabels(
                    invokeIndex, """
                        invoke-static {}, $FULLSCREEN->showFullscreenTitle()Z
                        move-result v$dummyRegister
                        if-eqz v$dummyRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(invokeIndex + 1))
                )
            }
        } ?: throw LayoutConstructorFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_FULLSCREEN_PANELS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-fullscreen-panels")

    }
}
