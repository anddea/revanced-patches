package app.revanced.patches.youtube.utils.fix.client.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.fix.client.fingerprints.CreatePlayerRequestBodyWithVersionReleaseFingerprint.indexOfBuildInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object CreatePlayerRequestBodyWithVersionReleaseFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("Google Inc."),
    customFingerprint = { methodDef, _ ->
        indexOfBuildInstruction(methodDef) >= 0
    },
) {
    fun indexOfBuildInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_VIRTUAL &&
                    reference?.name == "build" &&
                    reference.parameterTypes.isEmpty() &&
                    reference.returnType.startsWith("L")
        }
}
