package app.revanced.patches.music.general.oldstylelibraryshelf

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.patch.PatchList.RESTORE_OLD_STYLE_LIBRARY_SHELF
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val oldStyleLibraryShelfPatch = bytecodePatch(
    RESTORE_OLD_STYLE_LIBRARY_SHELF.title,
    RESTORE_OLD_STYLE_LIBRARY_SHELF.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        browseIdFingerprint.methodOrThrow().apply {
            val stringIndex = indexOfFirstStringInstructionOrThrow("FEmusic_offline")
            val targetIndex =
                indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.IGET_OBJECT)
            val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex + 1, """
                    invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->restoreOldStyleLibraryShelf(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$targetRegister
                    """
            )
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_restore_old_style_library_shelf",
            "false"
        )

        updatePatchStatus(RESTORE_OLD_STYLE_LIBRARY_SHELF)

    }
}
