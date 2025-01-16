package app.revanced.patches.reddit.layout.subredditdialog

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.REMOVE_SUBREDDIT_DIALOG
import app.revanced.patches.reddit.utils.settings.is_2024_41_or_greater
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
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

        frequentUpdatesSheetScreenFingerprint.methodOrThrow().apply {
            val index = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
            val register =
                getInstruction<OneRegisterInstruction>(index).registerA

            addInstruction(
                index,
                "invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->onDialogCreated(Landroid/view/View;)V"
            )
        }

        if (is_2024_41_or_greater) {
            val dismissReference = with (frequentUpdatesSheetV2ScreenInvokeFingerprint.methodOrThrow()) {
                val index = indexOfDismissScreenInstruction(this)
                getInstruction<ReferenceInstruction>(index).reference as MethodReference
            }

            findMethodOrThrow(EXTENSION_CLASS_DESCRIPTOR) {
                name == "dismissRedditDialogV2"
            }.addInstructions(
                0, """
                    check-cast p0, ${dismissReference.definingClass}
                    invoke-virtual {p0}, $dismissReference
                    """
            )

            frequentUpdatesSheetV2ScreenFingerprint
                .methodOrThrow()
                .apply {
                    val targetIndex = implementation!!.instructions.lastIndex

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {p0}, $EXTENSION_CLASS_DESCRIPTOR->dismissDialogV2(Ljava/lang/Object;)V
                            return-void
                            """
                    )
                    removeInstruction(targetIndex)
                }
        }

        // Not used in latest Reddit client.
        redditAlertDialogsFingerprint.second.methodOrNull?.apply {
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

        updatePatchStatus(
            "enableSubRedditDialog",
            REMOVE_SUBREDDIT_DIALOG
        )
    }
}
