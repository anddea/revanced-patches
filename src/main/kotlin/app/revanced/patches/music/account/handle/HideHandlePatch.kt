package app.revanced.patches.music.account.handle

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.account.handle.fingerprints.AccountSwitcherAccessibilityLabelFingerprint
import app.revanced.patches.music.account.handle.fingerprints.NamesInactiveAccountThumbnailSizeFingerprint
import app.revanced.patches.music.utils.integrations.Constants.ACCOUNT
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Hide handle",
    description = "Hides the handle in the account switcher.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object HideHandlePatch : BytecodePatch(
    setOf(
        AccountSwitcherAccessibilityLabelFingerprint,
        NamesInactiveAccountThumbnailSizeFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hide handle in account menu
         */
        AccountSwitcherAccessibilityLabelFingerprint.result?.let {
            it.mutableMethod.apply {
                val textColorIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "setTextColor"
                }

                for (index in textColorIndex until textColorIndex + 5) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_VIRTUAL) continue

                    if ((getInstruction<ReferenceInstruction>(index).reference as MethodReference).name == "setVisibility") {

                        val textViewInstruction = getInstruction<Instruction35c>(index)

                        replaceInstruction(
                            index,
                            "invoke-static {v${textViewInstruction.registerC}, v${textViewInstruction.registerD}}, $ACCOUNT->hideHandle(Landroid/widget/TextView;I)V"
                        )

                        break
                    }
                }
            }
        } ?: throw AccountSwitcherAccessibilityLabelFingerprint.exception

        /**
         * Hide handle in account switcher
         */
        NamesInactiveAccountThumbnailSizeFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $ACCOUNT->hideHandle(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw NamesInactiveAccountThumbnailSizeFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_handle",
            "true"
        )

    }
}
