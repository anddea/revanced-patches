package app.revanced.patches.music.utils.integrations.fingerprints

import app.revanced.patches.music.utils.integrations.fingerprints.InitFingerprint.indexOfGetProcessNameInstruction
import app.revanced.patches.shared.integrations.BaseIntegrationsPatch.IntegrationsFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object InitFingerprint : IntegrationsFingerprint(
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("activity"),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "onCreate"
                && indexOfGetProcessNameInstruction(methodDef) >= 0
    }
) {
    fun indexOfGetProcessNameInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_STATIC
                    && getReference<MethodReference>()?.name == "getProcessName"
        }
}