package app.revanced.patches.youtube.utils.fix.client.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * This is the fingerprint used in the 'client-spoof' patch around 2022.
 * (Integrated into 'UserAgentClientSpoofPatch' now.)
 *
 * This method is modified by 'UserAgentClientSpoofPatch', so the fingerprint does not check the [Opcode].
 */
internal object UserAgentHeaderBuilderFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    returnType = "Ljava/lang/String;",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;"),
    strings = listOf("(Linux; U; Android "),
)