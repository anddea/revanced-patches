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

package app.morphe.patches.music.video.playerresponse

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.string
import app.morphe.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private val PLAYER_PARAMETER_STARTS_WITH_PARAMETER_LIST = listOf(
    "Ljava/lang/String;", // VideoId.
    "[B",
    "Ljava/lang/String;", // Player parameters proto buffer.
    "Ljava/lang/String;", // PlaylistId.
    "I"                   // PlaylistIndex.
)

/**
 * For targets 7.03 and later.
 */
internal object PlayerParameterBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    filters = listOf(string("psps")),
    custom = custom@{ method, _ ->
        val parameterTypes = method.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize < 13) {
            return@custom false
        }

        val startsWithMethodParameterList = parameterTypes.slice(0..4)

        parametersEqual(
            PLAYER_PARAMETER_STARTS_WITH_PARAMETER_LIST,
            startsWithMethodParameterList
        )
    }
)

/**
 * The 8.51+ player-parameter builder keeps the playlist argument contract but no longer
 * contains the legacy player-service network phase marker used by [PlayerParameterBuilderFingerprint].
 */
internal object ModernPlayerParameterBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf(
        "Ljava/lang/String;", // VideoId.
        "[B",
        "Ljava/lang/String;", // Player parameters proto buffer.
        "Ljava/lang/String;", // PlaylistId.
        "I",                  // PlaylistIndex.
        "Z",
        "I",
        "L",
        "Ljava/util/Set;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "L",
        "Z",
        "Z",
        "Z",
        "Lj$/time/Duration;",
    ),
)

/**
 * For targets 7.02 and earlier.
 */
internal object PlayerParameterBuilderLegacyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf(
        "Ljava/lang/String;", // VideoId.
        "[B",
        "Ljava/lang/String;", // Player parameters proto buffer.
        "Ljava/lang/String;", // PlaylistId.
        "I",                  // PlaylistIndex.
        "I",
        "Ljava/util/Set;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "L",
        "Z",
        "Z", // Appears to indicate if the video id is being opened or is currently playing.
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_INTERFACE
    )
)
