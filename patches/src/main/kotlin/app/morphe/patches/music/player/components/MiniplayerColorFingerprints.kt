/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.music.player.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Matches the miniplayer method that animates its colors back to the color in its color field.
 * It compares the color last applied with the color field, and if they differ,
 * saves the color field as the color applied and animates to it for 200 Ms.
 * The second instruction match reads the color field.
 */
internal fun miniplayerResetColorsFingerprint(miniplayerClass: String) = Fingerprint(
    definingClass = miniplayerClass,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET,
            definingClass = "this",
            type = "I"
        ),
        fieldAccess(
            opcode = Opcode.IGET,
            definingClass = "this",
            type = "I",
            location = MatchAfterImmediately()
        ),
        literal(
            literal = 200L,
            location = MatchAfterImmediately()
        ),
        opcode(
            opcode = Opcode.IF_EQ,
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IPUT,
            definingClass = "this",
            type = "I",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * 9.03+
 */
internal object MiniplayerEmptyTextFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "mini_player_default_text")
    )
)
