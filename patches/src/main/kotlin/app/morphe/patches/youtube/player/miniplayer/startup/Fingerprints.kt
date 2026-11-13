/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

@file:Suppress("SpellCheckingInspection")

package app.morphe.patches.youtube.player.miniplayer.startup

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * 21.43+
 *
 * Matches the resume (Continue watching) check of the show miniplayer command.
 */
internal object ShowMiniplayerCommandFingerprint: Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            literal(164817L),
            literal(121253L)
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "L", "Ljava/util/Map;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            definingClass = "this",
            returnType = "Z"
        ),
        opcode(
            opcode = Opcode.MOVE_RESULT,
            location = MatchAfterImmediately()
        ),
        opcode(
            opcode = Opcode.IF_EQZ,
            location = MatchAfterImmediately()
        )
    )
)

/**
 * 21.42.
 *
 * Matches the resume (Continue watching) flag check of the show miniplayer command.
 */
internal object ShowMiniplayerCommandLegacy2142Fingerprint: Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            literal(164817L),
            literal(121253L)
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "L", "Ljava/util/Map;"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            type = "Z"
        ),
        opcode(
            opcode = Opcode.IF_EQZ,
            location = MatchAfterImmediately()
        )
    )
)

/**
 * 21.41 and lower.
 *
 * Matches the resume (Continue watching) flag check of the show miniplayer command.
 */
/**
 * Tested on YouTube 18.25.40 ~ 20.05.44
 *
 * This fingerprint is not compatible with YouTube 18.19.36 or earlier
 */
internal object ShowMiniplayerCommandLegacyFingerprint: Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        opcode(Opcode.IF_NEZ),
        opcode(
            opcode = Opcode.IF_EQZ,
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            type = "Z",
            location = MatchAfterWithin(5)
        ),
        opcode(
            opcode = Opcode.IF_EQZ,
            // 19.28 loads command IDs and an empty string between the flag and its branch.
            location = MatchAfterWithin(5)
        )
    ),
    // The command IDs occur in the opposite order in older clients.
    custom = { method, _ ->
        method.containsLiteralInstruction(164817L) && method.containsLiteralInstruction(121253L)
    }
)
