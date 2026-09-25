/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
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

package app.morphe.patches.music.general.landscapemode

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.music.utils.resourceid.isTablet
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object TabletIdentifierFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L"),
    filters = listOf(
        literal({ isTablet }),
        methodCall(
            definingClass = "Landroid/content/res/Resources;",
            name = "getBoolean",
            parameters = listOf("I"),
            returnType = "Z",
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

/**
 * Matches direct `Resources.getBoolean(R.bool.is_tablet)` checks.
 *
 * Newer YouTube Music versions inline this check in player and browse-layout code instead of
 * consistently calling [TabletIdentifierFingerprint]. ParentToolsActivity uses the same resource
 * for separate orientation behavior and must remain unchanged.
 */
internal object DirectTabletIdentifierFingerprint : Fingerprint(
    filters = listOf(
        literal({ isTablet }),
        methodCall(
            definingClass = "Landroid/content/res/Resources;",
            name = "getBoolean",
            parameters = listOf("I"),
            returnType = "Z",
            opcode = Opcode.INVOKE_VIRTUAL,
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
    custom = { _, classDef ->
        classDef.type != "Lcom/google/android/libraries/parenttools/youtube/ParentToolsActivity;"
    },
)

internal object LargeScreenIdentifierFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lrlj;",
            name = "b",
            parameters = listOf("Landroid/app/Activity;"),
            returnType = "Z",
            opcode = Opcode.INVOKE_STATIC,
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
