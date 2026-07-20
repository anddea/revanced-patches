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

package app.morphe.patches.shared.comments

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal val engagementPanelIdFingerprint = Fingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET,
        Opcode.CONST_16,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
    ),
    custom = { method, _ ->
        method.containsLiteralInstruction(18L)
    },
)

internal val engagementPanelRecyclerViewFingerprint = Fingerprint(
    returnType = "V",
    custom = { method, classDef ->
        !AccessFlags.STATIC.isSet(method.accessFlags) &&
                method.containsLiteralInstruction(49399797L) &&
                classDef.fields.find { field -> field.type == "Lcom/google/android/libraries/youtube/rendering/ui/widget/loadingframe/LoadingFrameLayout;" } != null &&
                classDef.fields.find { field -> field.type == "Lj$/util/Optional;" } != null &&
                indexOfRecyclerViewInstruction(method) >= 0 &&
                indexOfIfPresentInstruction(method) >= 0

    },
)

internal fun indexOfRecyclerViewInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.CHECK_CAST &&
                getReference<TypeReference>()?.type == "Landroid/support/v7/widget/RecyclerView;"
    }

internal fun indexOfIfPresentInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name?.startsWith("ifPresent") == true
    }

internal val engagementPanelTitleFingerprint = Fingerprint(
    custom = { method, _ ->
        method.containsLiteralInstruction(informationButton) &&
                method.containsLiteralInstruction(modernTitle) &&
                method.containsLiteralInstruction(title)
    }
)

internal val engagementPanelTitleParentFingerprint = Fingerprint(
    strings = listOf("[EngagementPanelTitleHeader] Cannot remove action buttons from header as the child count is out of sync. Buttons to remove exceed current header child count.")
)

internal val recyclerViewSmoothScrollToPositionFingerprint = Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    strings = listOf("Cannot smooth scroll without a LayoutManager set. Call setLayoutManager with a non-null argument.")
)
