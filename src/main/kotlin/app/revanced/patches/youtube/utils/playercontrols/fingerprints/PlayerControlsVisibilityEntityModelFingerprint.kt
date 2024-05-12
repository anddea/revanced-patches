package app.revanced.patches.youtube.utils.playercontrols.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PlayerControlsVisibilityEntityModelFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC.value,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.INVOKE_STATIC
    ),
    customFingerprint = { methodDef, _ -> methodDef.name == "getPlayerControlsVisibility" }
)