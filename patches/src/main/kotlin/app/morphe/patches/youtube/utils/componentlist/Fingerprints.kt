/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.utils.componentlist

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ComponentListFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            name = "nCopies",
        ),
    ),
)

private object ComponentContextParserFingerprint : Fingerprint(
    returnType = "L",
    filters = listOf(
        string("Failed to parse Element proto."),
        string("Cannot read theme key from model."),
    ),
)

internal object TreeNodeResultListFingerprint : Fingerprint(
    classFingerprint = ComponentContextParserFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            name = "nCopies",
        ),
    ),
)
