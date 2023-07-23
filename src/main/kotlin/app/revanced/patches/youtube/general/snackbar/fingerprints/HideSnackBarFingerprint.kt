package app.revanced.patches.youtube.general.snackbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object HideSnackBarFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/BottomUiContainer;") }
)