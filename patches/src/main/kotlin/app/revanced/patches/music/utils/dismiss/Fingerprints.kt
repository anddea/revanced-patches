package app.revanced.patches.music.utils.dismiss

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val dismissQueueFingerprint = legacyFingerprint(
    name = "dismissQueueFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    customFingerprint = { method, _ ->
        method.name == "handleDismissWatchEvent" &&
                indexOfDismissQueueInstruction(method) >= 0
    }
)

internal fun indexOfDismissQueueInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.definingClass?.endsWith("/MppWatchWhileLayout;") == true
    }
