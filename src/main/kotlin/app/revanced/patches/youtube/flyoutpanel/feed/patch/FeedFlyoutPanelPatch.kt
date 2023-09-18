package app.revanced.patches.youtube.flyoutpanel.feed.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.flyoutpanel.feed.fingerprints.BottomSheetMenuItemBuilderFingerprint
import app.revanced.patches.youtube.flyoutpanel.feed.fingerprints.ContextualMenuItemBuilderFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FLYOUT_PANEL
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch
@Name("Hide feed flyout panel")
@Description("Hides feed flyout panel components.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class FeedFlyoutPanelPatch : BytecodePatch(
    listOf(
        BottomSheetMenuItemBuilderFingerprint,
        ContextualMenuItemBuilderFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Phone
         */
        BottomSheetMenuItemBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                val targetParameter =
                    getInstruction<ReferenceInstruction>(targetIndex - 1).reference
                if (!targetParameter.toString().endsWith("Ljava/lang/CharSequence;"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $FLYOUT_PANEL->hideFeedFlyoutPanel(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$targetRegister
                        """
                )
            }
        } ?: throw BottomSheetMenuItemBuilderFingerprint.exception

        /**
         * Tablet
         */
        ContextualMenuItemBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val targetInstruction = getInstruction<Instruction35c>(targetIndex)

                val targetReferenceName =
                    (targetInstruction.reference as MethodReference).name
                if (targetReferenceName != "setText")
                    throw PatchException("Method name did not match: $targetReferenceName")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, $FLYOUT_PANEL->hideFeedFlyoutPanel(Landroid/widget/TextView;Ljava/lang/CharSequence;)V"
                )
            }
        } ?: throw ContextualMenuItemBuilderFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FLYOUT_PANEL_SETTINGS",
                "SETTINGS: HIDE_FEED_FLYOUT_PANEL"
            )
        )

        SettingsPatch.updatePatchStatus("hide-feed-flyout-panel")

    }
}
