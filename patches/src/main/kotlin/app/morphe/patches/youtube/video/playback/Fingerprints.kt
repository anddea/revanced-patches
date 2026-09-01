/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.video.playback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.shared.mapping.ResourceType.STRING
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.patches.youtube.video.information.VideoQualityChangedFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object DeviceDimensionsModelToStringFingerprint : Fingerprint(
    returnType = "L",
    strings = listOf("minh.", ";maxh.")
)

internal object PlaybackSpeedChangedFromRecyclerViewFingerprint : Fingerprint(
    classFingerprint = QualityChangedFromRecyclerViewFingerprint,
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

internal object ModernPlaybackSpeedChangedFromRecyclerViewFingerprint : Fingerprint(
    classFingerprint = VideoQualityChangedFingerprint,
    returnType = "L",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.INVOKE_VIRTUAL,
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
internal object PcmGetterMethodFingerprint : Fingerprint(
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

internal object LoadVideoParamsFingerprint : Fingerprint(
    classFingerprint = LoadVideoParamsParentFingerprint,
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

internal object LoadVideoParamsParentFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("J"),
    strings = listOf("LoadVideoParams.playerListener = null")
)

internal object QualityChangedFromRecyclerViewFingerprint : Fingerprint(
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

internal object ShowVideoQualityQuickMenuFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("VIDEO_QUALITIES_QUICK_MENU_BOTTOM_SHEET_FRAGMENT"),
    filters = listOf(
        opcode(Opcode.MOVE_RESULT),
        opcode(
            opcode = Opcode.IF_NEZ,
            location = MatchAfterWithin(3)
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "getSupportFragmentManager",
            location = MatchAfterWithin(3)
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("L", "Ljava/lang/String;"),
            returnType = "V",
            location = MatchAfterWithin(5)
        )
    )
)

internal object ShortsQualityMenuFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z"),
    returnType = "V",
    filters = listOf(
        resourceLiteral(STRING, "video_quality_unavailable_announcement")
    )
)

internal object ShortsQualityConstructorFingerprint : Fingerprint(
    classFingerprint = ShortsQualityMenuFingerprint,
    name = "<init>",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this"
        )
    )
)

internal object ShortsQualityChangeObserverPrimaryFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45387052)
    )
)

internal object ShortsQualityChangeObserverSecondaryFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45399743)
    )
)

internal object VideoQualityItemOnClickParentFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT")
)

internal object VideoQualityItemOnClickFingerprint : Fingerprint(
    classFingerprint = VideoQualityItemOnClickParentFingerprint,
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

internal object Vp9CapabilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    strings = listOf(
        "vp9_supported",
        "video/x-vnd.on2.vp9"
    )
)
