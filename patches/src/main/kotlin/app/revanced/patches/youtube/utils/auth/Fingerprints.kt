package app.revanced.patches.youtube.utils.auth

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val accountIdentityFingerprint = legacyFingerprint(
    name = "accountIdentityFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("${'$'}AutoValue_AccountIdentity;")
    }
)
