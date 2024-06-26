package app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.shorts.startupshortsreset.fingerprints.UserWasInShortsABConfigFingerprint.indexOfOptionalInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * This fingerprint is compatible with all YouTube versions after v18.15.40.
 */
internal object UserWasInShortsABConfigFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Failed to get offline response: "),
    customFingerprint = { methodDef, _ ->
        indexOfOptionalInstruction(methodDef) >= 0
    }
) {
    fun indexOfOptionalInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_STATIC
                    && getReference<MethodReference>().toString() == "Lj${'$'}/util/Optional;->of(Ljava/lang/Object;)Lj${'$'}/util/Optional;"
        }
}