package app.revanced.patches.youtube.player.seekbar

import app.revanced.patches.youtube.utils.resourceid.reelTimeBarPlayedColor
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val controlsOverlayStyleFingerprint = legacyFingerprint(
    name = "controlsOverlayStyleFingerprint",
    opcodes = listOf(Opcode.CONST_HIGH16),
    strings = listOf("YOUTUBE", "PREROLL", "POSTROLL"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/ControlsOverlayStyle;")
    }
)

internal val seekbarTappingFingerprint = legacyFingerprint(
    name = "seekbarTappingFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IPUT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.INT_TO_FLOAT,
        Opcode.INT_TO_FLOAT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    ),
    customFingerprint = { method, _ -> method.name == "onTouchEvent" }
)

internal val seekbarThumbnailsQualityFingerprint = legacyFingerprint(
    name = "seekbarThumbnailsQualityFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(45399684L),
)

internal val shortsSeekbarColorFingerprint = legacyFingerprint(
    name = "shortsSeekbarColorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(reelTimeBarPlayedColor),
)

internal val thumbnailPreviewConfigFingerprint = legacyFingerprint(
    name = "thumbnailPreviewConfigFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(45398577L),
)

internal val timeCounterFingerprint = legacyFingerprint(
    name = "timeCounterFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    returnType = "V",
    opcodes = listOf(
        Opcode.SUB_LONG_2ADDR,
        Opcode.IGET_WIDE,
        Opcode.SUB_LONG_2ADDR
    )
)

internal val timelineMarkerArrayFingerprint = legacyFingerprint(
    name = "timelineMarkerArrayFingerprint",
    returnType = "[Lcom/google/android/libraries/youtube/player/features/overlay/timebar/TimelineMarker;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
)
