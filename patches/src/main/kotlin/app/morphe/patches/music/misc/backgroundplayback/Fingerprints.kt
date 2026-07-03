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

package app.morphe.patches.music.misc.backgroundplayback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter.Companion.opcodesToFilters
import app.morphe.patcher.literal
import app.morphe.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object BackgroundPlaybackManagerFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L"),
    custom = { method, _ ->
        method.containsLiteralInstruction(64657230L)
    }
)

internal object DataSavingSettingsFragmentFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
    strings = listOf("pref_key_dont_play_nma_video"),
    custom = { method, _ ->
        method.definingClass.endsWith("/DataSavingSettingsFragment;") &&
                method.name == "onCreatePreferences"
    }
)

/**
 * Matches the kids playback policy before the YouTube Music 8.51 playback policy rewrite.
 *
 * The method has no stable literals in YouTube Music 6.20, so the older versions are matched by
 * the policy-state opcode sequence that was used before the feature flag was added.
 */
internal object KidsBackgroundPlaybackPolicyControllerLegacyFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I", "L", "Z"),
    filters = opcodesToFilters(
        Opcode.IGET,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.IF_NE,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQ,
        Opcode.GOTO,
        Opcode.RETURN_VOID,
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.IPUT_BOOLEAN
    )
)

/**
 * Matches the YouTube Music 8.51+ kids playback policy by its stable feature flag.
 *
 * The 9.15 policy method keeps the same `(I, L, Z) -> V` contract but no longer matches the old
 * policy-state opcode sequence.
 */
internal object KidsBackgroundPlaybackPolicyControllerFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I", "L", "Z"),
    filters = listOf(
        literal(45638079L)
    )
)

internal object MusicBrowserServiceFingerprint : Fingerprint(
    returnType = "L",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
    strings = listOf("android.service.media.extra.RECENT"),
    custom = { method, _ ->
        method.definingClass.endsWith("/MusicBrowserService;")
    },
)

internal object PodCastConfigFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    custom = { method, _ ->
        method.containsLiteralInstruction(45388403L)
    }
)
