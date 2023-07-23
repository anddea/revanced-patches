package app.revanced.patches.music.misc.sharebuttonhook.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object ShowToastFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/CharSequence;", "I"),
    opcodes = listOf(Opcode.IF_EQZ)
)
