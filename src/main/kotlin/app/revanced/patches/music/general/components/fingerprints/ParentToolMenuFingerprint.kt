package app.revanced.patches.music.general.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ParentToolMenuFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET,
    ),
    strings = listOf("pref_key_parent_tools"),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "onSettingsLoaded"
    }
)