package app.revanced.patches.youtube.utils.castbutton.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.MethodReferenceNameFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object MenuItemVisibilityFingerprint : MethodReferenceNameFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    reference = { "setVisible" }
)