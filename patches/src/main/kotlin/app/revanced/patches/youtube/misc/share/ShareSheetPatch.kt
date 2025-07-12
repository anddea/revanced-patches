package app.revanced.patches.youtube.misc.share

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.revanced.patches.youtube.utils.patch.PatchList.CHANGE_SHARE_SHEET
import app.revanced.patches.youtube.utils.resourceid.bottomSheetRecyclerView
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/ShareSheetPatch;"

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ShareSheetMenuFilter;"

@Suppress("unused")
val shareSheetPatch = bytecodePatch(
    CHANGE_SHARE_SHEET.title,
    CHANGE_SHARE_SHEET.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        sharedResourceIdPatch,
    )

    execute {

        // Detects that the Share sheet panel has been invoked.
        bottomSheetRecyclerViewFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(bottomSheetRecyclerView)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->onShareSheetMenuCreate(Landroid/support/v7/widget/RecyclerView;)V"
            )
        }

        // Remove the app list from the Share sheet panel on YouTube.
        updateShareSheetCommandFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.endIndex
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->overridePackageName(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS",
                "SETTINGS: CHANGE_SHARE_SHEET"
            ),
            CHANGE_SHARE_SHEET
        )

        // endregion

    }
}
