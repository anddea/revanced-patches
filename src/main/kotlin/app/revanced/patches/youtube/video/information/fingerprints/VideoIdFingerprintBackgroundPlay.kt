package app.revanced.patches.youtube.video.information.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.PlayerResponseModelUtils.indexOfPlayerResponseModelInstruction
import com.android.tools.smali.dexlib2.Opcode

/**
 * Renamed from VideoIdWithoutShortsFingerprint
 */
internal object VideoIdFingerprintBackgroundPlay : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.MONITOR_EXIT,
        Opcode.RETURN_VOID,
        Opcode.MONITOR_EXIT,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { methodDef, classDef ->
        methodDef.name == "l" &&
                classDef.methods.count() == 17 &&
                indexOfPlayerResponseModelInstruction(methodDef) >= 0
    }
)
