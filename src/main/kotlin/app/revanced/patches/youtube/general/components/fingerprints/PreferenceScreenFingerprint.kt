package app.revanced.patches.youtube.general.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.PreferenceScreenFingerprint.indexOfPreferenceScreenInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object PreferenceScreenFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf(":android:show_fragment_args"),
    customFingerprint = { methodDef, classDef ->
        AccessFlags.SYNTHETIC.isSet(classDef.accessFlags) &&
                indexOfPreferenceScreenInstruction(methodDef) >= 0
    }
) {
    fun indexOfPreferenceScreenInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstructionReversed {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_VIRTUAL &&
                    reference?.returnType == "Landroidx/preference/PreferenceScreen;" &&
                    reference.parameterTypes.size == 0
        }
}