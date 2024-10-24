package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.player.components.fingerprints.MppWatchWhileLayoutFingerprint.indexOfCallableInstruction
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MiniPlayerPlayPauseReplayButton
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.containsWideLiteralInstructionValue
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object MppWatchWhileLayoutFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.NEW_ARRAY),
    customFingerprint = custom@{ methodDef, _ ->
        if (!methodDef.definingClass.endsWith("/MppWatchWhileLayout;")) {
            return@custom false
        }
        if (methodDef.name != "onFinishInflate") {
            return@custom false
        }
        if (!methodDef.containsWideLiteralInstructionValue(MiniPlayerPlayPauseReplayButton)) {
            return@custom false
        }
        if (!SettingsPatch.upward0718) {
            return@custom true
        }

        indexOfCallableInstruction(methodDef) >= 0
    }
) {
    fun indexOfCallableInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_VIRTUAL &&
                    reference?.returnType == "V" &&
                    reference.parameterTypes.size == 1 &&
                    reference.parameterTypes.firstOrNull() == "Ljava/util/concurrent/Callable;"
        }
}
