package app.morphe.patches.music.utils.sponsorblock

import app.morphe.patches.music.utils.resourceid.inlineTimeBarAdBreakMarkerColor
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val musicPlaybackControlsTimeBarDrawFingerprint = legacyFingerprint(
    name = "musicPlaybackControlsTimeBarDrawFingerprint",
    returnType = "V",
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/MusicPlaybackControlsTimeBar;") &&
                method.name == "draw"
    }
)

internal val musicPlaybackControlsTimeBarOnMeasureFingerprint = legacyFingerprint(
    name = "musicPlaybackControlsTimeBarOnMeasureFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/MusicPlaybackControlsTimeBar;") &&
                method.name == "onMeasure"
    }
)

internal val rectangleFieldInvalidatorFingerprint = legacyFingerprint(
    name = "rectangleFieldInvalidatorFingerprint",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE
    ),
    customFingerprint = { method, _ ->
        indexOfInvalidateInstruction(method) >= 0
    }
)

internal fun indexOfInvalidateInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        getReference<MethodReference>()?.name == "invalidate"
    }

internal val seekBarConstructorFingerprint = legacyFingerprint(
    name = "seekBarConstructorFingerprint",
    returnType = "V",
    literals = listOf(inlineTimeBarAdBreakMarkerColor),
)

internal val seekbarOnDrawFingerprint = legacyFingerprint(
    name = "seekbarOnDrawFingerprint",
    customFingerprint = { method, _ -> method.name == "onDraw" }
)