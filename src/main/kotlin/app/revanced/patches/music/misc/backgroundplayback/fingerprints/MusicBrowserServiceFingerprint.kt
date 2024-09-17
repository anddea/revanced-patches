package app.revanced.patches.music.misc.backgroundplayback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.misc.backgroundplayback.fingerprints.MusicBrowserServiceFingerprint.indexOfMBSInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal object MusicBrowserServiceFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
    customFingerprint = custom@{ methodDef, _ ->
        if (!methodDef.definingClass.endsWith("/MusicBrowserService;"))
            return@custom false

        indexOfMBSInstruction(methodDef) >= 0
    }
) {
    fun indexOfMBSInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.CONST_STRING &&
                    getReference<StringReference>()?.string?.startsWith("MBS: Return empty root for client: %s") == true
        }
}