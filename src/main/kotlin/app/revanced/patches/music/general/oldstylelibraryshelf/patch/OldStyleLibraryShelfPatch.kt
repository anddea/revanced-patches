package app.revanced.patches.music.general.oldstylelibraryshelf.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.general.oldstylelibraryshelf.fingerprints.BrowseIdFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Enable old style library shelf")
@Description("Return the library shelf to old style.")
@DependsOn([SettingsPatch::class])
@MusicCompatibility
class OldStyleLibraryShelfPatch : BytecodePatch(
    listOf(BrowseIdFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        BrowseIdFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getStringIndex("FEmusic_offline") - 5
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $MUSIC_GENERAL->enableOldStyleLibraryShelf(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        } ?: throw BrowseIdFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_enable_old_style_library_shelf",
            "false"
        )

    }
}