package app.morphe.patches.music.utils.extension.hooks

import app.morphe.patcher.Fingerprint
import app.morphe.patches.shared.extension.extensionHook
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val applicationInitHook = extensionHook(
    fingerprint = Fingerprint(
        returnType = "V",
        parameters = listOf(),
        strings = listOf("activity"),
        custom = { method, _ ->
            method.name == "onCreate" &&
                    method.indexOfFirstInstruction {
                        opcode == Opcode.INVOKE_VIRTUAL
                                && getReference<MethodReference>()?.name == "getRunningAppProcesses"
                    } >= 0
        }
    )
)
