/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - ILoveOpenSourceApplications (https://github.com/ILoveOpenSourceApplications)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.music.general.startpage

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.music.utils.patch.PatchList.CHANGE_START_PAGE
import app.morphe.patches.music.utils.playservice.is_6_27_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addListPreference
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
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

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

        addListPreference(
            CategoryType.GENERAL,
            "revanced_change_start_page"
        )

        updatePatchStatus(CHANGE_START_PAGE)

    }
}
