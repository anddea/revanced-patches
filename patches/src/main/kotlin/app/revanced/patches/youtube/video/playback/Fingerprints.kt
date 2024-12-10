package app.revanced.patches.youtube.video.playback

import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val av1CodecFingerprint = legacyFingerprint(
    name = "av1CodecFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    returnType = "L",
    strings = listOf("AtomParsers", "video/av01"),
    customFingerprint = { method, _ ->
        method.returnType != "Ljava/util/List;" &&
                method.containsLiteralInstruction(1987076931L)
    }
)

internal val byteBufferArrayFingerprint = legacyFingerprint(
    name = "byteBufferArrayFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "I",
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.SHL_INT_LIT8,
        Opcode.SHL_INT_LIT8,
        Opcode.OR_INT_2ADDR,
        Opcode.SHL_INT_LIT8,
        Opcode.OR_INT_2ADDR,
        Opcode.OR_INT_2ADDR,
        Opcode.RETURN
    )
)

internal val byteBufferArrayParentFingerprint = legacyFingerprint(
    name = "byteBufferArrayParentFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "C",
    parameters = listOf("Ljava/nio/charset/Charset;", "[C")
)

internal val deviceDimensionsModelToStringFingerprint = legacyFingerprint(
    name = "deviceDimensionsModelToStringFingerprint",
    returnType = "L",
    strings = listOf("minh.", ";maxh.")
)

internal val hdrCapabilityFingerprint = legacyFingerprint(
    name = "hdrCapabilityFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf(
        "av1_profile_main_10_hdr_10_plus_supported",
        "video/av01"
    )
)

internal val playbackSpeedChangedFromRecyclerViewFingerprint = legacyFingerprint(
    name = "playbackSpeedChangedFromRecyclerViewFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.INVOKE_VIRTUAL
    )
)

internal val playbackSpeedInitializeFingerprint = legacyFingerprint(
    name = "playbackSpeedInitializeFingerprint",
    returnType = "F",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.RETURN
    )
)

internal val qualityChangedFromRecyclerViewFingerprint = legacyFingerprint(
    name = "qualityChangedFromRecyclerViewFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET,  // Video resolution (human readable).
        Opcode.IGET_OBJECT,
        Opcode.IGET_BOOLEAN,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_DIRECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.GOTO,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
    )
)

internal val qualitySetterFingerprint = legacyFingerprint(
    name = "qualitySetterFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT")
)

internal val vp9CapabilityFingerprint = legacyFingerprint(
    name = "vp9CapabilityFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    strings = listOf(
        "vp9_supported",
        "video/x-vnd.on2.vp9"
    )
)
