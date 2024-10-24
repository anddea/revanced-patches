package app.revanced.patches.shared.settingmenu.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object RemovePreferenceFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroidx/preference/Preference;"),
    opcodes = listOf(Opcode.INVOKE_VIRTUAL),
    customFingerprint = custom@{ methodDef, _ ->
        if (methodDef.definingClass != "Landroidx/preference/PreferenceGroup;") {
            return@custom false
        }
        val instructions = methodDef.implementation?.instructions ?: return@custom false
        instructions.elementAt(0).opcode == Opcode.INVOKE_DIRECT
    }
)