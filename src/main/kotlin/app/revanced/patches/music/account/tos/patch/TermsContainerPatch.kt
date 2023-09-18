package app.revanced.patches.music.account.tos.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.account.tos.fingerprints.TermsOfServiceFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.TosFooter
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_ACCOUNT
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("Hide terms container")
@Description("Hides terms of service container at the account menu.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class TermsContainerPatch : BytecodePatch(
    listOf(TermsOfServiceFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        TermsOfServiceFingerprint.result?.let {
            it.mutableMethod.apply {
                val tosIndex = getWideLiteralIndex(TosFooter)
                var insertIndex = 0

                for (index in tosIndex until implementation!!.instructions.size) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_VIRTUAL) continue

                    val targetReference =
                        getInstruction<ReferenceInstruction>(index).reference.toString()

                    if (targetReference.endsWith("/PrivacyTosFooter;->setVisibility(I)V")) {
                        insertIndex = index

                        val visibilityRegister =
                            getInstruction<Instruction35c>(insertIndex).registerD

                        addInstruction(
                            index + 1,
                            "const/4 v$visibilityRegister, 0x0"
                        )
                        addInstructions(
                            index, """
                                invoke-static {}, $MUSIC_ACCOUNT->hideTermsContainer()I
                                move-result v$visibilityRegister
                                """
                        )

                        break
                    }
                }
                if (insertIndex == 0)
                    throw PatchException("target Instruction not found!")
            }
        } ?: throw TermsOfServiceFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.ACCOUNT,
            "revanced_hide_terms_container",
            "false"
        )

    }
}
