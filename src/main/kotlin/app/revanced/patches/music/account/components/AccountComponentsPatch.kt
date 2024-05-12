package app.revanced.patches.music.account.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.music.account.components.fingerprints.AccountSwitcherAccessibilityLabelFingerprint
import app.revanced.patches.music.account.components.fingerprints.MenuEntryFingerprint
import app.revanced.patches.music.account.components.fingerprints.NamesInactiveAccountThumbnailSizeFingerprint
import app.revanced.patches.music.account.components.fingerprints.TermsOfServiceFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.ACCOUNT_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object AccountComponentsPatch : BaseBytecodePatch(
    name = "Hide account components",
    description = "Adds the options to hide components related to account menu.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AccountSwitcherAccessibilityLabelFingerprint,
        MenuEntryFingerprint,
        NamesInactiveAccountThumbnailSizeFingerprint,
        TermsOfServiceFingerprint,
    )
) {
    override fun execute(context: BytecodeContext) {

        // region patch for hide account menu

        MenuEntryFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val textIndex = getTargetIndexWithMethodReferenceName("setText")
                val viewIndex = getTargetIndexWithMethodReferenceName("addView")

                val textRegister = getInstruction<FiveRegisterInstruction>(textIndex).registerD
                val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerD

                addInstruction(
                    textIndex + 1,
                    "invoke-static {v$textRegister, v$viewRegister}, $ACCOUNT_CLASS_DESCRIPTOR->hideAccountMenu(Ljava/lang/CharSequence;Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide handle

        // account menu
        AccountSwitcherAccessibilityLabelFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {

                val textColorIndex = getTargetIndexWithMethodReferenceName("setTextColor")
                val setVisibilityIndex = getTargetIndexWithMethodReferenceName(textColorIndex, "setVisibility")
                val textViewInstruction = getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

                replaceInstruction(
                    setVisibilityIndex,
                    "invoke-static {v${textViewInstruction.registerC}, v${textViewInstruction.registerD}}, $ACCOUNT_CLASS_DESCRIPTOR->hideHandle(Landroid/widget/TextView;I)V"
                )
            }
        }

        // account switcher
        NamesInactiveAccountThumbnailSizeFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $ACCOUNT_CLASS_DESCRIPTOR->hideHandle(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide terms container

        TermsOfServiceFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndexWithReference("/PrivacyTosFooter;->setVisibility(I)V")
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
        }

        // endregion

        SettingsPatch.addSwitchPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu",
            "false"
        )
        SettingsPatch.addPreferenceWithIntent(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu_filter_strings",
            "revanced_hide_account_menu"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu_empty_component",
            "false",
            "revanced_hide_account_menu"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_handle",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_terms_container",
            "false"
        )
    }
}
