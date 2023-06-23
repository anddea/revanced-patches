package app.revanced.patches.reddit.misc.openlink.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object ScreenNavigatorFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L", "L", "Z"),
    opcodes = listOf(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC
    ),
    strings = listOf("uri", "android.intent.action.VIEW"),
    customFingerprint = { it, _ -> it.definingClass.endsWith("/RedditScreenNavigator;") }
)