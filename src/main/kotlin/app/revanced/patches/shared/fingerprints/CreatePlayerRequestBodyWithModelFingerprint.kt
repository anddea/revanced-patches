package app.revanced.patches.shared.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.shared.fingerprints.CreatePlayerRequestBodyWithModelFingerprint.indexOfModelInstruction
import app.revanced.patches.shared.fingerprints.CreatePlayerRequestBodyWithModelFingerprint.indexOfReleaseInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object CreatePlayerRequestBodyWithModelFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.OR_INT_LIT16),
    customFingerprint = { methodDef, _ ->
        indexOfModelInstruction(methodDef) >= 0
                && indexOfReleaseInstruction(methodDef) >= 0
    }
) {
    fun indexOfModelInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            getReference<FieldReference>().toString() == "Landroid/os/Build;->MODEL:Ljava/lang/String;"
        }

    fun indexOfReleaseInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            getReference<FieldReference>().toString() == "Landroid/os/Build${'$'}VERSION;->RELEASE:Ljava/lang/String;"
        }
}
