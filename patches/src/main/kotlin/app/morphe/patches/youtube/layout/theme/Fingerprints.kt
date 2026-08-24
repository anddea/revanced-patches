/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.layout.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import com.android.tools.smali.dexlib2.Opcode

/**
 * The pivot bar creates the view stub of the new content dot, and of the count next to it. The
 * count is matched as well because the effects picker uses the same dot id.
 */
internal object PivotBarNewContentDotFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.ID, "new_content_dot"),
        methodCall(
            name = "findViewById",
            location = MatchAfterImmediately()
        ),
        opcode(
            opcode = Opcode.CHECK_CAST,
            location = MatchAfterWithin(3)
        ),
        resourceLiteral(
            ResourceType.ID,
            "new_content_count",
            location = MatchAfterWithin(30)
        ),
        opcode(opcode = Opcode.CHECK_CAST)
    )
)

/**
 * The top bar creates the view stub of the new content count of the notification button, and of
 * the dot next to it. The count comes first, which is the other way around than the pivot bar,
 * and that is what keeps both fingerprints from matching the same method.
 */
internal object TopBarNewContentCountFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.ID, "new_content_count"),
        methodCall(
            name = "findViewById",
            location = MatchAfterImmediately()
        ),
        opcode(
            opcode = Opcode.CHECK_CAST,
            location = MatchAfterWithin(3)
        ),
        resourceLiteral(
            ResourceType.ID,
            "new_content_dot",
            location = MatchAfterWithin(30)
        ),
        methodCall(
            name = "findViewById",
            location = MatchAfterImmediately()
        ),
        opcode(
            opcode = Opcode.CHECK_CAST,
            location = MatchAfterWithin(3)
        )
    )
)
