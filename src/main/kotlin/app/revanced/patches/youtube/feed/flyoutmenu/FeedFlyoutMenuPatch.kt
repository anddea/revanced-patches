package app.revanced.patches.youtube.feed.flyoutmenu

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.youtube.feed.flyoutmenu.fingerprints.BottomSheetMenuItemBuilderFingerprint
import app.revanced.patches.youtube.feed.flyoutmenu.fingerprints.BottomSheetMenuItemBuilderLegacyFingerprint
import app.revanced.patches.youtube.feed.flyoutmenu.fingerprints.ContextualMenuItemBuilderFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.FEED_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object FeedFlyoutMenuPatch : BaseBytecodePatch(
    name = "Hide feed flyout menu",
    description = "Adds the ability to hide feed flyout menu components using a custom filter.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        BottomSheetMenuItemBuilderFingerprint,
        BottomSheetMenuItemBuilderLegacyFingerprint,
        ContextualMenuItemBuilderFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Phone
         */
        val bottomSheetMenuItemBuilderResult = BottomSheetMenuItemBuilderLegacyFingerprint.result
            ?: BottomSheetMenuItemBuilderFingerprint.resultOrThrow()

        bottomSheetMenuItemBuilderResult.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                val targetParameter =
                    getInstruction<ReferenceInstruction>(targetIndex - 1).reference
                if (!targetParameter.toString().endsWith("Ljava/lang/CharSequence;"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $FEED_CLASS_DESCRIPTOR->hideFlyoutMenu(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        /**
         * Tablet
         */
        ContextualMenuItemBuilderFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val targetInstruction = getInstruction<Instruction35c>(targetIndex)

                val targetReferenceName =
                    (targetInstruction.reference as MethodReference).name
                if (targetReferenceName != "setText")
                    throw PatchException("Method name did not match: $targetReferenceName")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                            "$FEED_CLASS_DESCRIPTOR->hideFlyoutMenu(Landroid/widget/TextView;Ljava/lang/CharSequence;)V"
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: FEED",
                "SETTINGS: HIDE_FEED_FLYOUT_MENU"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
