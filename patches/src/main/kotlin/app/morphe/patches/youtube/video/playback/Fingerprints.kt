package app.morphe.patches.youtube.video.playback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patches.shared.mapping.ResourceType.LAYOUT
import app.morphe.patches.shared.mapping.ResourceType.STRING
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object deviceDimensionsModelToStringFingerprint : Fingerprint(
    returnType = "L",
    strings = listOf("minh.", ";maxh.")
)

internal object playbackSpeedChangedFromRecyclerViewFingerprint : Fingerprint(
    classFingerprint = qualityChangedFromRecyclerViewFingerprint,
    returnType = "L",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.INVOKE_VIRTUAL
    ),
    custom = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.IGET &&
                    getReference<FieldReference>()?.type == "F"
        } >= 0
    }
)

// Fingerprint for the METHOD that returns PlayerConfigModel
private const val PCM_GETTER_FIELD_TYPE = "Lcom/google/android/libraries/youtube/innertube/model/media/PlayerConfigModel;"
internal object pcmGetterMethodFingerprint : Fingerprint(
    returnType = PCM_GETTER_FIELD_TYPE,
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(Opcode.IGET_OBJECT, Opcode.RETURN_OBJECT),
    custom = custom@{ method, _ ->
        val instructions = method.instructionsOrNull
        if (instructions == null || instructions.count() != 2) return@custom false

        ((method.instructionsOrNull?.firstOrNull() as? ReferenceInstruction)?.reference
                as? FieldReference)?.type == PCM_GETTER_FIELD_TYPE
    }
)

internal object loadVideoParamsFingerprint : Fingerprint(
    classFingerprint = loadVideoParamsParentFingerprint,
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.IPUT,
        Opcode.INVOKE_INTERFACE,
    )
)

internal object loadVideoParamsParentFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("J"),
    strings = listOf("LoadVideoParams.playerListener = null")
)

internal object qualityChangedFromRecyclerViewFingerprint : Fingerprint(
    returnType = "L",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    custom = { method, _ ->
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

internal object videoQualityMenuOptionsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC),
    returnType = "[L",
    parameters = listOf("Landroid/content/Context", "L", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_NEZ,
    ) + resourceLiteral(STRING, "video_quality_quick_menu_advanced_menu_description")
)

internal object videoQualityMenuViewInflateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L", "L", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_SUPER,
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_16,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    ) + resourceLiteral(LAYOUT, "video_quality_bottom_sheet_list_fragment_title")
)

internal object videoQualityItemOnClickParentFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT")
)

internal object videoQualityItemOnClickFingerprint : Fingerprint(
    classFingerprint = videoQualityItemOnClickParentFingerprint,
    returnType = "V",
    parameters = listOf(
        "Landroid/widget/AdapterView;",
        "Landroid/view/View;",
        "I",
        "J"
    ),
    custom = { method, _ ->
        method.name == "onItemClick"
    }
)

internal object vp9CapabilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    strings = listOf(
        "vp9_supported",
        "video/x-vnd.on2.vp9"
    )
)
