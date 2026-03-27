package app.morphe.patches.music.account.components

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.extension.Constants.ACCOUNT_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.patch.PatchList.HIDE_ACCOUNT_COMPONENTS
import app.morphe.patches.music.utils.resourceid.channelHandle
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addPreferenceWithIntent
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val accountComponentsPatch = bytecodePatch(
    HIDE_ACCOUNT_COMPONENTS.title,
    HIDE_ACCOUNT_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
    )

    execute {

        // region patch for hide account menu

        menuEntryFingerprint.methodOrThrow().apply {
            val textIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setText"
            }
            val viewIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "addView"
            }

            val textRegister = getInstruction<FiveRegisterInstruction>(textIndex).registerD
            val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerD

            addInstruction(
                textIndex + 1,
                "invoke-static {v$textRegister, v$viewRegister}, " +
                        "$ACCOUNT_CLASS_DESCRIPTOR->hideAccountMenu(Ljava/lang/CharSequence;Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for hide handle

        // account menu
        accountSwitcherAccessibilityLabelFingerprint.methodOrThrow().apply {
            val textColorIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setTextColor"
            }
            val setVisibilityIndex = indexOfFirstInstructionOrThrow(textColorIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setVisibility"
            }
            val textViewInstruction =
                getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

            replaceInstruction(
                setVisibilityIndex,
                "invoke-static {v${textViewInstruction.registerC}, v${textViewInstruction.registerD}}, " +
                        "$ACCOUNT_CLASS_DESCRIPTOR->hideHandle(Landroid/widget/TextView;I)V"
            )
        }

        // account switcher
        val textViewField = with(
            channelHandleFingerprint
                .methodOrThrow(namesInactiveAccountThumbnailSizeFingerprint)
        ) {
            val literalIndex = indexOfFirstLiteralInstructionOrThrow(channelHandle)
            getInstruction(
                indexOfFirstInstructionOrThrow(literalIndex) {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type == "Landroid/widget/TextView;"
                },
            ).getReference<FieldReference>()
        }

        namesInactiveAccountThumbnailSizeFingerprint.methodOrThrow().apply {
            var hook = false

            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference
                    instruction.opcode == Opcode.IGET_OBJECT &&
                            reference is FieldReference &&
                            reference == textViewField
                }
                .map { (index, _) -> index }
                .forEach { index ->
                    val insertIndex = index - 1
                    if (!hook && getInstruction(insertIndex).opcode == Opcode.IF_NEZ) {
                        val insertRegister =
                            getInstruction<OneRegisterInstruction>(insertIndex).registerA

                        addInstructions(
                            insertIndex, """
                                invoke-static {v$insertRegister}, $ACCOUNT_CLASS_DESCRIPTOR->hideHandle(Z)Z
                                move-result v$insertRegister
                                """
                        )
                        hook = true
                    }
                }

            if (!hook) {
                throw PatchException("Could not find TextUtils.isEmpty() index")
            }
        }

        // endregion

        // region patch for hide terms container

        termsOfServiceFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_VIRTUAL &&
                        reference?.name == "setVisibility" &&
                        reference.definingClass.endsWith("/PrivacyTosFooter;")
            }
            val visibilityRegister =
                getInstruction<FiveRegisterInstruction>(insertIndex).registerD

            addInstruction(
                insertIndex + 1,
                "const/4 v$visibilityRegister, 0x0"
            )
            addInstructions(
                insertIndex, """
                    invoke-static {}, $ACCOUNT_CLASS_DESCRIPTOR->hideTermsContainer()I
                    move-result v$visibilityRegister
                    """
            )

        }

        // endregion

        addSwitchPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu_filter_strings",
            "revanced_hide_account_menu"
        )
        addSwitchPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu_empty_component",
            "false",
            "revanced_hide_account_menu"
        )
        addSwitchPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_handle",
            "true"
        )
        addSwitchPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_terms_container",
            "false"
        )

        updatePatchStatus(HIDE_ACCOUNT_COMPONENTS)

    }
}
