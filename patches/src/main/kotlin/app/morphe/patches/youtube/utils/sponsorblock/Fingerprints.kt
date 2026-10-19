/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 */

package app.morphe.patches.youtube.utils.sponsorblock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val rectangleFieldInvalidatorFingerprint = Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    custom = { method, _ ->
        indexOfInvalidateInstruction(method) >= 0
    }
)

internal val segmentPlaybackControllerFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Ljava/lang/Object;"),
    filters = OpcodesFilter.opcodesToFilters(Opcode.CONST_STRING),
    custom = { method, _ ->
        method.definingClass == "$EXTENSION_PATH/sponsorblock/SegmentPlaybackController;"
                && method.name == "setSponsorBarRect"
    }
)

internal fun indexOfInvalidateInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        getReference<MethodReference>()?.name == "invalidate"
    }

/**
 * The shared timebar dispatches event 3 when scrubbing finishes. Calling this method routes
 * seeking through the active Shorts controller, including versions with preloaded players.
 */
internal val shortsSeekCompleteFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = listOf("J"),
    returnType = "V",
    filters = listOf(
        literal(3),
        methodCall(parameters = listOf("I", "J"), returnType = "V"),
    ),
)

internal val shortsIsScrubbingFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IGET_BOOLEAN,
        Opcode.RETURN,
    ),
)
