package app.revanced.patches.music.misc.premium.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object MembershipSettingsFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/CharSequence;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList()
)
