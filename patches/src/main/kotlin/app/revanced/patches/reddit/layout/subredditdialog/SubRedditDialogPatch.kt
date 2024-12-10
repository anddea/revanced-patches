package app.revanced.patches.reddit.layout.subredditdialog

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.REMOVE_SUBREDDIT_DIALOG
import app.revanced.patches.reddit.utils.resourceid.cancelButton
import app.revanced.patches.reddit.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.reddit.utils.resourceid.textAppearanceRedditBaseOldButtonColored
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/RemoveSubRedditDialogPatch;"

@Suppress("unused")
val subRedditDialogPatch = bytecodePatch(
    REMOVE_SUBREDDIT_DIALOG.title,
    REMOVE_SUBREDDIT_DIALOG.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch
    )

    execute {
        frequentUpdatesSheetScreenFingerprint.methodOrThrow().apply {
            val cancelButtonViewIndex =
                indexOfFirstLiteralInstructionOrThrow(cancelButton) + 2
            val cancelButtonViewRegister =
                getInstruction<OneRegisterInstruction>(cancelButtonViewIndex).registerA

            addInstruction(
                cancelButtonViewIndex + 1,
                "invoke-static {v$cancelButtonViewRegister}, $EXTENSION_CLASS_DESCRIPTOR->dismissDialog(Landroid/view/View;)V"
            )
        }

        redditAlertDialogsFingerprint.methodOrThrow().apply {
            val insertIndex =
                indexOfFirstLiteralInstructionOrThrow(
                    textAppearanceRedditBaseOldButtonColored
                ) + 1
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
