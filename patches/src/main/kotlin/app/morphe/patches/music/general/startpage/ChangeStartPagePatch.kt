package app.morphe.patches.music.general.startpage

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.music.utils.patch.PatchList.CHANGE_START_PAGE
import app.morphe.patches.music.utils.playservice.is_6_27_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addPreferenceWithIntent
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.util.addEntryValues
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/ChangeStartPagePatch;"

private val changeStartPageResourcePatch = resourcePatch(
    description = "changeStartPageResourcePatch"
) {
    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        fun appendStartPage(startPage: String) {
            addEntryValues(
                "revanced_change_start_page_entries",
                "@string/revanced_change_start_page_entry_$startPage",
            )
            addEntryValues(
                "revanced_change_start_page_entry_values",
                startPage.uppercase(),
            )
        }

        if (is_6_27_or_greater) {
            appendStartPage("search")
        }
        appendStartPage("subscriptions")
    }
}

@Suppress("unused")
val changeStartPagePatch = bytecodePatch(
    CHANGE_START_PAGE.title,
    CHANGE_START_PAGE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        changeStartPageResourcePatch,
        settingsPatch,
    )

    execute {

        coldStartIntentFingerprint.methodOrThrow().addInstruction(
            0,
            "invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->overrideIntent(Landroid/content/Intent;)V"
        )

        coldStartUpFingerprint.methodOrThrow().apply {
            val defaultBrowseIdIndex = indexOfFirstStringInstructionOrThrow(DEFAULT_BROWSE_ID)
            val browseIdIndex = indexOfFirstInstructionReversedOrThrow(defaultBrowseIdIndex) {
                opcode == Opcode.IGET_OBJECT &&
                        getReference<FieldReference>()?.type == "Ljava/lang/String;"
            }
            val browseIdRegister = getInstruction<TwoRegisterInstruction>(browseIdIndex).registerA

            addInstructions(
                browseIdIndex + 1, """
                    invoke-static {v$browseIdRegister}, $EXTENSION_CLASS_DESCRIPTOR->overrideBrowseId(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$browseIdRegister
                    """
            )
        }

        addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_change_start_page"
        )

        updatePatchStatus(CHANGE_START_PAGE)

    }
}
