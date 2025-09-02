package app.revanced.patches.youtube.general.signintotvpopup

import app.revanced.patches.youtube.utils.resourceid.mdxSeamlessTVSignInDrawerFragmentTitle
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val signInToTvPopupFingerprint = legacyFingerprint(
    name = "signInToTvPopupFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(mdxSeamlessTVSignInDrawerFragmentTitle),
    customFingerprint = { method, _ ->
        method.parameterTypes.firstOrNull() == "Ljava/lang/String;"
    }
)
