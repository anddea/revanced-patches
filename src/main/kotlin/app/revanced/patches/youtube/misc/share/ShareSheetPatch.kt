package app.revanced.patches.youtube.misc.share

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.misc.share.fingerprints.BottomSheetRecyclerViewFingerprint
import app.revanced.patches.youtube.misc.share.fingerprints.UpdateShareSheetCommandFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.BottomSheetRecyclerView
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object ShareSheetPatch : BaseBytecodePatch(
    name = "Change share sheet",
    description = "Add option to change from in-app share sheet to system share sheet.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        BottomSheetRecyclerViewFingerprint,
        UpdateShareSheetCommandFingerprint,
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/ShareSheetPatch;"

    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ShareSheetMenuFilter;"

    override fun execute(context: BytecodeContext) {

        // Detects that the Share sheet panel has been invoked.
        BottomSheetRecyclerViewFingerprint.resultOrThrow().mutableMethod.apply {
            val constIndex = indexOfFirstWideLiteralInstructionValueOrThrow(BottomSheetRecyclerView)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->onShareSheetMenuCreate(Landroid/support/v7/widget/RecyclerView;)V"
            )
        }

        // Remove the app list from the Share sheet panel on YouTube.
        UpdateShareSheetCommandFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->overridePackageName(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS",
                "SETTINGS: CHANGE_SHARE_SHEET"
            )
        )
    }
}