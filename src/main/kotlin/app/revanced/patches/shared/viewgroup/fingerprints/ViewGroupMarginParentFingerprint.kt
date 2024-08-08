package app.revanced.patches.shared.viewgroup.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ViewGroupMarginParentFingerprint : MethodFingerprint(
    returnType = "Landroid/view/ViewGroup${'$'}LayoutParams;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Ljava/lang/Class;", "Landroid/view/ViewGroup${'$'}LayoutParams;"),
    strings = listOf("SafeLayoutParams"),
)