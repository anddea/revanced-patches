package app.revanced.patches.music.general.oldstylelibraryshelf

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.general.oldstylelibraryshelf.fingerprints.BrowseIdFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object OldStyleLibraryShelfPatch : BaseBytecodePatch(
    name = "Restore old style library shelf",
    description = "Adds an option to return the library tab to the old style.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(BrowseIdFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        BrowseIdFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val stringIndex = getStringInstructionIndex("FEmusic_offline")
                val targetIndex = getTargetIndexReversed(stringIndex, Opcode.IGET_OBJECT)
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->restoreOldStyleLibraryShelf(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_restore_old_style_library_shelf",
            "false"
        )

    }
}