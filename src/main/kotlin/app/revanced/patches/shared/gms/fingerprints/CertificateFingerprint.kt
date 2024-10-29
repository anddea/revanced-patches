package app.revanced.patches.shared.gms.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.shared.gms.fingerprints.CertificateFingerprint.indexOfGetPackageNameInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Method which the package name is used to check the app signature.
 */
internal object CertificateFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("X.509", "user", "S"),
    customFingerprint = { methodDef, _ ->
        indexOfGetPackageNameInstruction(methodDef) >= 0
    }
) {
    fun indexOfGetPackageNameInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            getReference<MethodReference>()?.toString() == "Landroid/content/Context;->getPackageName()Ljava/lang/String;"
        }
}
