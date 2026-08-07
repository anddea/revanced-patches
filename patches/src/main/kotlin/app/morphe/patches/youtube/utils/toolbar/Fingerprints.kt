package app.morphe.patches.youtube.utils.toolbar

import app.morphe.patcher.Fingerprint
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.AccessFlags

internal object ToolBarPatchFingerprint : Fingerprint(
    definingClass = "$UTILS_PATH/ToolBarPatch;",
    name = "hookToolBar",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/view/View;")
)

internal object ToolBarPatchWithImageViewFingerprint : Fingerprint(
    definingClass = "$UTILS_PATH/ToolBarPatch;",
    name = "hookToolBarWithImageView",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/String;",
        "Landroid/view/View;",
        "Landroid/widget/ImageView;"
    )
)
