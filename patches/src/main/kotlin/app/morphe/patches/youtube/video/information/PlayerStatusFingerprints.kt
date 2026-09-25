/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.video.information

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PlayerStatusEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "NEW",
        "PLAYBACK_PENDING",
        "PLAYBACK_LOADED",
        "PLAYBACK_INTERRUPTED",
        "INTERSTITIAL_REQUESTED",
        "INTERSTITIAL_PLAYING",
        "VIDEO_PLAYING",
        "ENDED",
    )
)

internal fun getPlayerStatusFingerprint(playerStatusEnumClass: String) = object : Fingerprint(
    classFingerprint = PlayerInitFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(playerStatusEnumClass),
    filters = listOf(
        // The opcode for the first index of the method is sget-object.
        // Even in sufficiently old versions, such as YT 17.34, the opcode for the first index is sget-object.
        opcode(Opcode.SGET_OBJECT),
        methodCall(
            definingClass = "Lj$/time/Instant;",
            name = "plus"
        )
    )
) {}

