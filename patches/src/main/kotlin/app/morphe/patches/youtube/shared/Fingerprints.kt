/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 */

package app.morphe.patches.youtube.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object WatchNextResponseParserFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/util/List;",
    filters = listOf(
        literal(49399797L),
        opcode(Opcode.SGET_OBJECT),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterImmediately()
        ),
        literal(51779735L),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/Object;",
            location = MatchAfterWithin(5)
        ),
        opcode(
            Opcode.CHECK_CAST,
            location = MatchAfterImmediately()
        ),
        literal(46659098L),
    )
)
