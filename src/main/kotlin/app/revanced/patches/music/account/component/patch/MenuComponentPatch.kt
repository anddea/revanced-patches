package app.revanced.patches.music.account.component.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.account.component.fingerprints.MenuEntryFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_ACCOUNT
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch
@Name("Hide account menu")
@Description("Hide account menu elements.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class MenuComponentPatch : BytecodePatch(
    listOf(MenuEntryFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        MenuEntryFingerprint.result?.let {
            it.mutableMethod.apply {
                val textIndex = targetIndex("setText")
                val viewIndex = targetIndex("addView")

                val textRegister = getInstruction<FiveRegisterInstruction>(textIndex).registerD
                val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerD

                addInstruction(
                    textIndex + 1,
                    "invoke-static {v$textRegister, v$viewRegister}, $MUSIC_ACCOUNT->hideAccountMenu(Ljava/lang/CharSequence;Landroid/view/View;)V"
                )
            }
        } ?: throw MenuEntryFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu_filter_strings",
            "revanced_hide_account_menu"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_account_menu_empty_component",
            "false",
            "revanced_hide_account_menu"
        )
    }

    private companion object {
        fun MutableMethod.targetIndex(descriptor: String): Int {
            return implementation?.let {
                it.instructions.indexOfFirst { instruction ->
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == descriptor
                }
            } ?: throw PatchException("No Method Implementation found!")
        }
    }
}
