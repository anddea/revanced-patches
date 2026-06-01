package app.morphe.patches.youtube.player.flyoutmenu.hide

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
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

/**
 * This fingerprint is compatible with YouTube v18.39.xx+
 */
internal object PipModeConfigFingerprint : Fingerprint(
    filters = listOf(
        literal(45427407L),
    ),
)
