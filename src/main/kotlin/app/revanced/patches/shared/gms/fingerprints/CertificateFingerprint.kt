package app.revanced.patches.shared.gms.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.shared.gms.fingerprints.CertificateFingerprint.GET_PACKAGE_NAME_METHOD_REFERENCE
import app.revanced.util.fingerprint.ReferenceFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Method which the package name is used to check the app signature.
 */
internal object CertificateFingerprint : ReferenceFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("X.509", "user", "S"),
    reference = { GET_PACKAGE_NAME_METHOD_REFERENCE }
) {
    const val GET_PACKAGE_NAME_METHOD_REFERENCE =
        "Landroid/content/Context;->getPackageName()Ljava/lang/String;"
}
