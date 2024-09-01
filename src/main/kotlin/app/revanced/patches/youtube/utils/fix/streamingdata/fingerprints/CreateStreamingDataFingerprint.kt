package app.revanced.patches.youtube.utils.fix.streamingdata.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object CreateStreamingDataFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IPUT_OBJECT
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.SGET_OBJECT &&
                    getReference<FieldReference>()?.name == "playerThreedRenderer"
        } >= 0
    },
)