package app.morphe.patches.youtube.general.snackbar

import app.morphe.patcher.Fingerprint
import app.morphe.patches.youtube.utils.resourceid.insetElementsWrapper
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val BOTTOM_UI_CONTAINER_CLASS_DESCRIPTOR =
    "Lcom/google/android/apps/youtube/app/common/ui/bottomui/BottomUiContainer;"

internal val bottomUiContainerFingerprint = legacyFingerprint(
    name = "bottomUiContainerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    customFingerprint = { _, classDef ->
        classDef.type == BOTTOM_UI_CONTAINER_CLASS_DESCRIPTOR
    }
)

internal val bottomUiContainerThemeFingerprint = legacyFingerprint(
    name = "bottomUiContainerThemeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(BOTTOM_UI_CONTAINER_CLASS_DESCRIPTOR),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.CONST,
    ),
)

internal val lithoSnackBarFingerprint = legacyFingerprint(
    name = "lithoSnackBarFingerprint",
    returnType = "Landroid/view/View;",
    literals = listOf(insetElementsWrapper),
    customFingerprint = { method, _ ->
        indexOfBackGroundColor(method) >= 0
    }
)

internal fun indexOfBackGroundColor(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setBackgroundColor"
    }

internal object LegacySnackBarConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/quantum/snackbar/Snackbar;",
    name = "<init>",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(
        "Landroid/content/Context;",
        "Landroid/util/AttributeSet;",
        "I"
    )
)

internal object YouTubeSnackBarConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/common/ui/bottomui/YouTubeSnackbar;",
    name = "<init>",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(
        "Landroid/content/Context;",
        "Landroid/util/AttributeSet;",
        "I"
    )
)
