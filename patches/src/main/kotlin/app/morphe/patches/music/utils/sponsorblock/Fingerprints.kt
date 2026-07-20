/*
 * Portions of this file are adapted from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.music.utils.sponsorblock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter.Companion.opcodesToFilters
import app.morphe.patcher.literal
import app.morphe.patches.music.utils.resourceid.inlineTimeBarAdBreakMarkerColor
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Matches {@code MusicPlaybackControlsTimeBar.draw(Canvas)} for compact-player markers. */
internal object MusicPlaybackControlsTimeBarDrawFingerprint : Fingerprint(
    definingClass = "/MusicPlaybackControlsTimeBar;",
    name = "draw",
    returnType = "V"
)

/** Matches {@code MusicPlaybackControlsTimeBar.onMeasure(int, int)} to locate its track rect. */
internal object MusicPlaybackControlsTimeBarOnMeasureFingerprint : Fingerprint(
    definingClass = "/MusicPlaybackControlsTimeBar;",
    name = "onMeasure",
    returnType = "V"
)

internal object SeekBarConstructorFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(literal(inlineTimeBarAdBreakMarkerColor))
)

internal object RectangleFieldInvalidatorFingerprint : Fingerprint(
    classFingerprint = SeekBarConstructorFingerprint,
    filters = opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE
    ),
    custom = { method, _ -> indexOfInvalidateInstruction(method) >= 0 }
)

internal fun indexOfInvalidateInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        getReference<MethodReference>()?.name == "invalidate"
    }

internal object SeekbarOnDrawFingerprint : Fingerprint(
    classFingerprint = SeekBarConstructorFingerprint,
    name = "onDraw"
)
