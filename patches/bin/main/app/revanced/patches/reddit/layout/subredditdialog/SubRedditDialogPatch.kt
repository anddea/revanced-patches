package app.revanced.patches.reddit.layout.subredditdialog

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.REMOVE_SUBREDDIT_DIALOG
import app.revanced.patches.reddit.utils.settings.is_2024_41_or_greater
import app.revanced.patches.reddit.utils.settings.is_2025_01_or_greater
import app.revanced.patches.reddit.utils.settings.is_2025_05_or_greater
import app.revanced.patches.reddit.utils.settings.is_2025_06_or_greater
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/RemoveSubRedditDialogPatch;"

@Suppress("unused")
val subRedditDialogPatch = bytecodePatch(
    REMOVE_SUBREDDIT_DIALOG.title,
    REMOVE_SUBREDDIT_DIALOG.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        if (is_2024_41_or_greater) {
            frequentUpdatesHandlerFingerprint
                .methodOrThrow()
                .apply {
                    listOfIsLoggedInInstruction(this)
                        .forEach { index ->
                            val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                            addInstructions(
                                index + 2, """
                                    invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->spoofLoggedInStatus(Z)Z
                                    move-result v$register
                                    """
                            )
                        }
                }
        }

        // Not used in latest Reddit client.
        if (!is_2025_05_or_greater) {
            frequentUpdatesSheetScreenFingerprint.methodOrThrow().apply {
                val index = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
                val register =
                    getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index,
                    "invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->dismissDialog(Landroid/view/View;)V"
                )
            }
        }

        if (is_2025_01_or_greater) {
            nsfwAlertEmitFingerprint.methodOrThrow().apply {
                val hasBeenVisitedIndex = indexOfHasBeenVisitedInstruction(this)
                val hasBeenVisitedRegister =
                    getInstruction<OneRegisterInstruction>(hasBeenVisitedIndex + 1).registerA

                addInstructions(
                    hasBeenVisitedIndex + 2, """
                        invoke-static {v$hasBeenVisitedRegister}, $EXTENSION_CLASS_DESCRIPTOR->spoofHasBeenVisitedStatus(Z)Z
                        move-result v$hasBeenVisitedRegister
                        """
                )
            }
        }

        // Not used in latest Reddit client.
        if (!is_2025_06_or_greater) {
            redditAlertDialogsFingerprint.methodOrThrow().apply {
                val backgroundTintIndex = indexOfSetBackgroundTintListInstruction(this)
                val insertIndex =
                    indexOfFirstInstructionOrThrow(backgroundTintIndex) {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.name == "setTextAppearance"
                    }
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->confirmDialog(Landroid/widget/TextView;)V"
                )
            }
        }

        updatePatchStatus(
            "enableSubRedditDialog",
            REMOVE_SUBREDDIT_DIALOG
        )
    }
}
