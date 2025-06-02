package app.revanced.patches.youtube.video.playback

import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

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
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.INVOKE_VIRTUAL
    ),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.IGET &&
                    getReference<FieldReference>()?.type == "F"
        } >= 0
    }
)

// Fingerprint for the METHOD that returns PlayerConfigModel
private const val PCM_GETTER_FIELD_TYPE = "Lcom/google/android/libraries/youtube/innertube/model/media/PlayerConfigModel;"
val pcmGetterMethodFingerprint = legacyFingerprint(
    name = "pcmGetterMethodFingerprint",
    returnType = PCM_GETTER_FIELD_TYPE,
    parameters = listOf(),
    opcodes = listOf(Opcode.IGET_OBJECT, Opcode.RETURN_OBJECT),
    customFingerprint = custom@{ method, _ ->
        val instructions = method.instructionsOrNull
        if (instructions == null || instructions.count() != 2) return@custom false

        ((method.instructionsOrNull?.firstOrNull() as? ReferenceInstruction)?.reference
                as? FieldReference)?.type == PCM_GETTER_FIELD_TYPE
    }
)

internal val loadVideoParamsFingerprint = legacyFingerprint(
    name = "loadVideoParamsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.IPUT,
        Opcode.INVOKE_INTERFACE,
    )
)

internal val loadVideoParamsParentFingerprint = legacyFingerprint(
    name = "loadVideoParamsParentFingerprint",
    returnType = "Z",
    parameters = listOf("J"),
    strings = listOf("LoadVideoParams.playerListener = null")
)

internal val qualityChangedFromRecyclerViewFingerprint = legacyFingerprint(
    name = "qualityChangedFromRecyclerViewFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    customFingerprint = { method, classDef ->
        method.implementation?.instructions?.any { insn ->
            insn.opcode == Opcode.NEW_INSTANCE &&
                    (insn as? ReferenceInstruction)?.reference?.toString() == "Lcom/google/android/libraries/youtube/innertube/model/media/VideoQuality;"
        } == true &&
                method.implementation?.instructions?.any { insn ->
                    insn.opcode == Opcode.CONST_4 &&
                            (insn as? NarrowLiteralInstruction)?.narrowLiteral == 2
                } == true
    }
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
