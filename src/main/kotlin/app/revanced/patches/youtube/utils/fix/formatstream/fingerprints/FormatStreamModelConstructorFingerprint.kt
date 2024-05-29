package app.revanced.patches.youtube.utils.fix.formatstream.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.fix.formatstream.fingerprints.FormatStreamModelConstructorFingerprint.indexOfParseInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object FormatStreamModelConstructorFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    opcodes = listOf(
        Opcode.IGET_OBJECT,     // get formatStreamData
        Opcode.INVOKE_STATIC    // Uri.parse(String formatStreamData)
    ),
    customFingerprint = { methodDef, classDef ->
        classDef.type == "Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;" &&
                indexOfParseInstruction(methodDef) >= 0
    }
) {
   fun indexOfParseInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_STATIC && getReference<MethodReference>()?.name == "parse"
        }
}

