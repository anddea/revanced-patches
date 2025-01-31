package app.revanced.patches.music.utils.navigation

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
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
