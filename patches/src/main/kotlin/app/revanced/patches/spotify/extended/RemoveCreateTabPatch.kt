package app.revanced.patches.spotify.extended

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.fingerprint
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.getResourceId
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal val addCreateTabMethodFingerprint = fingerprint {
    custom { method, _ ->
        method.containsLiteralInstruction(getResourceId(STRING, "bottom_navigation_bar_create_tab_title"))
    }
}

internal val removeCreateTabMethodFingerprint = fingerprint {
    custom { method, _ ->
        method.containsLiteralInstruction(getResourceId(STRING, "navigationbar_musicappitems_create_title"))
    }
}

@Suppress("unused")
val removeCreateTabPatch = bytecodePatch(
    name = "Remove Create tab",
    description = "Removes the 'Create' (Plus) tab from the bottom navigation bar.",
) {
    compatibleWith("com.spotify.music")
    dependsOn(resourceMappingPatch)

    execute {
        addCreateTabMethodFingerprint.method.addInstruction(0, "return-void")

        val targetMethod = removeCreateTabMethodFingerprint.method

        val lastInstructionIndex = targetMethod.instructions.count() - 1
        if (lastInstructionIndex < 0) {
            throw PatchException("Method ${targetMethod.name} found by fingerprint has no instructions.")
        }

        val lastInstruction = targetMethod.getInstruction(lastInstructionIndex)
        if (lastInstruction.opcode != Opcode.RETURN_OBJECT) {
            throw PatchException("Expected last instruction to be RETURN_OBJECT in ${targetMethod.name}, but found ${lastInstruction.opcode.name}. Patch logic needs update.")
        }

        val nullIndex = targetMethod.indexOfFirstInstructionOrThrow {
            opcode == Opcode.CONST_4 && (this as NarrowLiteralInstruction).narrowLiteral == 0
        }

        val nullRegister = targetMethod.getInstruction<OneRegisterInstruction>(nullIndex).registerA

        val newReturnInstruction = "return-object v$nullRegister"
        targetMethod.replaceInstruction(lastInstructionIndex, newReturnInstruction)
    }
}
