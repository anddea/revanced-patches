package app.revanced.patches.shared.gms.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.util.MethodUtil

internal object PrimesApiFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("PrimesApiImpl.java"),
    customFingerprint = { methodDef, _ ->
        MethodUtil.isConstructor(methodDef)
    }
)