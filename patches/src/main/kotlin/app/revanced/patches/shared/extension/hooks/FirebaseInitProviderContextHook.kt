package app.revanced.patches.shared.extension.hooks

import app.revanced.patches.shared.extension.extensionHook
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private var getResourcesIndex = -1

internal val firebaseInitProviderContextHook = extensionHook(
    insertIndexResolver = { method ->
        getResourcesIndex = indexOfGerResourcesInstruction(method)

        getResourcesIndex + 2
    },
    contextRegisterResolver = { method ->
        val overrideInstruction =
            method.implementation!!.instructions.elementAt(getResourcesIndex)
                    as FiveRegisterInstruction

        "v${overrideInstruction.registerC}"
    },
) {
    strings("firebase_database_url")
    custom { method, _ ->
        indexOfGerResourcesInstruction(method) >= 0
    }
}

private fun indexOfGerResourcesInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.toString() =="Landroid/content/Context;->getResources()Landroid/content/res/Resources;"
    }
