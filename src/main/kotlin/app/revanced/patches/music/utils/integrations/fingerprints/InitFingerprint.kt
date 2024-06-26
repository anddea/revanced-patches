package app.revanced.patches.music.utils.integrations.fingerprints

import app.revanced.patches.shared.integrations.BaseIntegrationsPatch.IntegrationsFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object InitFingerprint : IntegrationsFingerprint(
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("activity"),
    customFingerprint = handler@{ methodDef, _ ->
        if (methodDef.name != "onCreate")
            return@handler false

        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL
                    && getReference<MethodReference>()?.name == "getRunningAppProcesses"
        } >= 0
    }
)