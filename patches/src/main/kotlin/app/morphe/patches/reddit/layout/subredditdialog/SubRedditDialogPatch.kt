package app.morphe.patches.reddit.layout.subredditdialog

import app.morphe.patches.reddit.utils.extension.Constants
import app.morphe.patches.reddit.utils.patch.PatchList
import app.morphe.patches.reddit.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.reddit.utils.settings.is_2024_41_or_greater
import app.morphe.patches.reddit.utils.settings.is_2025_01_or_greater
import app.morphe.patches.reddit.utils.settings.is_2025_05_or_greater
import app.morphe.patches.reddit.utils.settings.is_2025_06_or_greater
import app.morphe.patches.reddit.utils.settings.settingsPatch
import app.morphe.patches.reddit.utils.settings.updatePatchStatus
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "${Constants.PATCHES_PATH}/RemoveSubRedditDialogPatch;"

@Suppress("unused")
val subRedditDialogPatch = bytecodePatch(
    PatchList.REMOVE_SUBREDDIT_DIALOG.title,
    PatchList.REMOVE_SUBREDDIT_DIALOG.summary,
) {
    compatibleWith(app.morphe.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
    )

    execute {

        if (is_2024_41_or_greater) {
            frequentUpdatesHandlerFingerprint
                .methodOrThrow()
                .apply {
                    listOfIsLoggedInInstruction(this)
                        .forEach { index ->
                            val register =
                                getInstruction<OneRegisterInstruction>(index + 1).registerA

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

            var hookCount = 0

            nsfwAlertBuilderFingerprint.mutableClassOrThrow().let {
                it.methods.forEach { method ->
                    method.apply {
                        val showIndex = indexOfFirstInstruction {
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    getReference<MethodReference>()?.name == "show"
                        }
                        if (showIndex >= 0) {
                            val dialogRegister =
                                getInstruction<OneRegisterInstruction>(showIndex + 1).registerA

                            addInstruction(
                                showIndex + 2,
                                "invoke-static {v$dialogRegister}, $EXTENSION_CLASS_DESCRIPTOR->dismissNSFWDialog(Ljava/lang/Object;)V"
                            )
                            hookCount++
                        }
                    }
                }
            }

            if (hookCount == 0) {
                throw PatchException("Failed to find hook method")
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
            PatchList.REMOVE_SUBREDDIT_DIALOG
        )
    }
}
