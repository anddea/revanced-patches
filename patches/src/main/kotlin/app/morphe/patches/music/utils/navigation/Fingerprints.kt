package app.morphe.patches.music.utils.navigation

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val tabLayoutViewSetSelectedFingerprint = legacyFingerprint(
    name = "tabLayoutViewSetSelectedFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("I"),
    customFingerprint = { method, classDef ->
        classDef.type == "Lcom/google/android/material/tabs/TabLayout;" &&
                indexOfChildAtInstruction(method) >= 0 &&
                indexOfSetViewActivatedInstruction(method) >= 0
    }
)

internal fun indexOfChildAtInstruction(method: Method) = method.indexOfFirstInstruction {
    opcode == Opcode.INVOKE_VIRTUAL && getReference<MethodReference>()?.name == "getChildAt"
}

private fun indexOfSetViewActivatedInstruction(method: Method) = method.indexOfFirstInstruction {
    opcode == Opcode.INVOKE_VIRTUAL && getReference<MethodReference>()?.name == "setActivated"
}
