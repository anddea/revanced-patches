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

package app.morphe.patches.music.video.information

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.parametersMatch
import app.morphe.patcher.string
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val playerControllerSetTimeReferenceFingerprint = legacyFingerprint(
    name = "playerControllerSetTimeReferenceFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.IGET_OBJECT
    ),
    strings = listOf("Media progress reported outside media playback: ")
)

internal val videoEndFingerprint = legacyFingerprint(
    name = "videoEndFingerprint",
    strings = listOf("Attempting to seek during an ad")
)

internal val videoIdFingerprint = legacyFingerprint(
    name = "videoIdFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/String;"),
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("Null initialPlayabilityStatus")
)

/** Modern player seek method used by YouTube Music 8.51+. */
internal object ModernVideoEndFingerprint : Fingerprint(
    parameters = listOf("J", "L"),
    filters = listOf(
        string("currentPositionMs."),
        string(";seekTimeUs.")
    )
)

/** Modern track-load callback, including the extra boolean introduced after 9.15. */
internal object ModernVideoIdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(string("Null initialPlayabilityStatus")),
    custom = { method, _ ->
        parametersMatch(method.parameters, listOf("L", "Ljava/lang/String;")) ||
                parametersMatch(method.parameters, listOf("L", "Ljava/lang/String;", "Z"))
    }
)

internal object ModernPlayerControllerSetTimeReferenceFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.IGET_OBJECT
    ),
    strings = listOf("Media progress reported outside media playback: ")
)
