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

package app.morphe.patches.youtube.player.flyoutmenu.hide

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.patches.youtube.utils.YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object CaptionsOldBottomSheetLayoutInflaterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "bottom_sheet_title"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "findViewById",
            location = MatchAfterWithin(3),
        ),
        resourceLiteral(ResourceType.STRING, "subtitle_menu_settings_footer_info"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/widget/ListView;->addFooterView(Landroid/view/View;Ljava/lang/Object;Z)V",
        ),
    ),
)

internal object QualityOldBottomSheetLayoutInflaterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "video_quality_bottom_sheet_list_fragment_title"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/widget/ListView;->addHeaderView(Landroid/view/View;Ljava/lang/Object;Z)V",
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/widget/ListView;->addFooterView(Landroid/view/View;Ljava/lang/Object;Z)V",
        ),
    ),
)

internal object CurrentVideoFormatConstructorFingerprint : Fingerprint(
    classFingerprint = CurrentVideoFormatToStringFingerprint,
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    custom = { method, _ ->
        indexOfVideoQualitiesInstruction(method) >= 0
    },
)

internal fun indexOfVideoQualitiesInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.IPUT_OBJECT &&
                getReference<FieldReference>()?.type == "[$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE"
    }

internal object CurrentVideoFormatToStringFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    strings = listOf("currentVideoFormat="),
    custom = { method, _ ->
        method.name == "toString"
    },
)

internal object DefaultOverflowOverlayOnClickFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/player/features/overlay/overflow/ui/DefaultOverflowOverlay;",
    name = "onClick",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        opcode(Opcode.IF_NE),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            location = MatchAfterWithin(2),
        ),
    ),
)

internal fun getCurrentVideoFormatConstructorFingerprint(
    videoQualityArray: String,
) = object : Fingerprint(
    classFingerprint = CurrentVideoFormatToStringFingerprint,
    name = "<init>",
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = videoQualityArray,
        ),
    ),
) {}

/**
 * This fingerprint is compatible with YouTube v18.39.xx+
 */
internal object PipModeConfigFingerprint : Fingerprint(
    filters = listOf(
        literal(45427407L),
    ),
)
