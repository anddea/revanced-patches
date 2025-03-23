package app.revanced.patches.music.general.startpage

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.GENERAL_PATH
import app.revanced.patches.music.utils.patch.PatchList.CHANGE_START_PAGE
import app.revanced.patches.music.utils.playservice.is_6_27_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.util.addEntryValues
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
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
