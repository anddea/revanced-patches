package app.morphe.patches.youtube.video.playback

import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val deviceDimensionsModelToStringFingerprint = legacyFingerprint(
    name = "deviceDimensionsModelToStringFingerprint",
    returnType = "L",
    strings = listOf("minh.", ";maxh.")
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
    customFingerprint = { method, _ ->
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

internal val qualityMenuViewInflateOnItemClickFingerprint = legacyFingerprint(
    name = "qualityMenuViewInflateOnItemClickFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { method, _ ->
        method.name == "onItemClick" &&
                indexOfContextInstruction(method) >= 0
    }
)

internal fun indexOfContextInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.IGET_OBJECT &&
                getReference<FieldReference>()?.type == "Landroid/content/Context;"
    }


internal val videoQualityItemOnClickParentFingerprint = legacyFingerprint(
    name = "videoQualityItemOnClickParentFingerprint",
    returnType = "V",
    strings = listOf("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT")
)

internal val videoQualityItemOnClickFingerprint = legacyFingerprint(
    name = "videoQualityItemOnClickFingerprint",
    returnType = "V",
    parameters = listOf(
        "Landroid/widget/AdapterView;",
        "Landroid/view/View;",
        "I",
        "J"
    ),
    customFingerprint = { method, _ ->
        method.name == "onItemClick"
    }
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
