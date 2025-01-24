package app.revanced.patches.shared.extension.hooks

import app.revanced.patches.shared.extension.extensionHook
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private var initIndex = -1
private var isRange = true

internal val cronetEngineContextHook = extensionHook(
    insertIndexResolver = { method ->
        initIndex = method.indexOfFirstInstruction(Opcode.INVOKE_DIRECT_RANGE)

        if (initIndex < 0) {
            initIndex = method.indexOfFirstInstructionOrThrow(Opcode.INVOKE_DIRECT)
            isRange = false
        }

        initIndex
    },
    contextRegisterResolver = { method ->
        val initInstruction =
            method.implementation!!.instructions.elementAt(initIndex)
        if (isRange) {
            val overrideInstruction = initInstruction as Instruction3rc
            "v${overrideInstruction.startRegister + 1}"
        } else {
            val overrideInstruction = initInstruction as FiveRegisterInstruction
            "v${overrideInstruction.registerD}"
        }
    },
) {
    returns("Lorg/chromium/net/CronetEngine;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    strings("Could not create CronetEngine")
    custom { method, classDef ->
        method.indexOfFirstInstruction {
            (opcode == Opcode.INVOKE_DIRECT || opcode == Opcode.INVOKE_DIRECT_RANGE) &&
                    getReference<MethodReference>()?.parameterTypes?.firstOrNull() == "Landroid/content/Context;"
        } >= 0
    }
}
