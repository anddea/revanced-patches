/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
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

package app.morphe.patches.music.general.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.music.utils.resourceid.chipCloud
import app.morphe.patches.music.utils.resourceid.musicTasteBuilderShelf
import app.morphe.patches.music.utils.resourceid.playerOverlayChip
import app.morphe.patches.music.utils.resourceid.searchButton
import app.morphe.patches.music.utils.resourceid.toolTipContentView
import app.morphe.patches.music.utils.resourceid.topBarMenuItemImageView
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val chipCloudFingerprint = legacyFingerprint(
    name = "chipCloudFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literals = listOf(chipCloud),
)

internal val contentPillFingerprint = legacyFingerprint(
    name = "contentPillFingerprint",
    returnType = "V",
    strings = listOf("Content pill VE is null")
)

internal val floatingButtonFingerprint = legacyFingerprint(
    name = "floatingButtonFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(Opcode.AND_INT_LIT16)
)

internal val floatingButtonParentFingerprint = legacyFingerprint(
    name = "floatingButtonParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.INVOKE_DIRECT),
    literals = listOf(259982244L),
)

/** Matches the history menu item before and after the 9.x menu-class split. */
internal object HistoryMenuItemFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/view/Menu;"),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "history_menu_item"),
        methodCall(smali = "Landroid/view/MenuItem;->setVisible(Z)Landroid/view/MenuItem;"),
        opcode(Opcode.RETURN_VOID, MatchAfterImmediately())
    ),
    custom = { _, classDef ->
        classDef.methods.count() == 4 || classDef.methods.count() == 5
    }
)

internal object HistoryMenuItemOfflineTabFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/view/Menu;"),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "offline_settings_menu_item"),
        resourceLiteral(ResourceType.ID, "history_menu_item"),
        methodCall(smali = "Landroid/view/MenuItem;->setVisible(Z)Landroid/view/MenuItem;"),
        opcode(Opcode.RETURN_VOID, MatchAfterImmediately())
    )
)

internal val mediaRouteButtonFingerprint = legacyFingerprint(
    name = "mediaRouteButtonFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    strings = listOf("MediaRouteButton")
)

internal val playerOverlayChipFingerprint = legacyFingerprint(
    name = "playerOverlayChipFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(playerOverlayChip),
)

internal val searchActionViewFingerprint = legacyFingerprint(
    name = "searchActionViewFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(searchButton),
    customFingerprint = { _, classDef ->
        classDef.type.endsWith("/SearchActionProvider;")
    }
)

internal val searchBarFingerprint = legacyFingerprint(
    name = "searchBarFingerprint",
    returnType = "V",
    customFingerprint = { method, _ ->
        indexOfVisibilityInstruction(method) >= 0
    }
)

fun indexOfVisibilityInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setVisibility"
    }

internal val searchBarParentFingerprint = legacyFingerprint(
    name = "searchBarParentFingerprint",
    returnType = "Landroid/content/Intent;",
    strings = listOf("web_search")
)

internal const val SOUND_SEARCH_BUTTON_FEATURE_FLAG = 45625491L

/**
 * This fingerprint is compatible with YouTube Music 6.48.52 ~ 8.04.53.
 */
internal val soundSearchLegacyFingerprint = legacyFingerprint(
    name = "soundSearchLegacyFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(SOUND_SEARCH_BUTTON_FEATURE_FLAG),
)

/**
 * This fingerprint is compatible with YouTube Music 6.48.52+.
 */
internal val soundSearchFingerprint = legacyFingerprint(
    name = "soundSearchFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.INVOKE_INTERFACE),
)

/**
 * This fingerprint is compatible with YouTube Music 6.48.52+.
 */
internal val soundSearchConstructorFingerprint = legacyFingerprint(
    name = "soundSearchConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.STATIC or AccessFlags.CONSTRUCTOR,
    parameters = emptyList(),
    literals = listOf(208485L),
)

internal val tasteBuilderConstructorFingerprint = legacyFingerprint(
    name = "tasteBuilderConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(musicTasteBuilderShelf),
)

internal val tasteBuilderSyntheticFingerprint = legacyFingerprint(
    name = "tasteBuilderSyntheticFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    parameters = listOf("L", "Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT
    )
)

internal val tooltipContentViewFingerprint = legacyFingerprint(
    name = "tooltipContentViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(toolTipContentView),
)

internal val topBarMenuItemImageViewFingerprint = legacyFingerprint(
    name = "topBarMenuItemImageViewFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(topBarMenuItemImageView),
)
