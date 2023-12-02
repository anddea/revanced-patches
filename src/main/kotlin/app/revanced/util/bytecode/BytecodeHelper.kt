package app.revanced.util.bytecode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH
import app.revanced.util.integrations.Constants.UTILS_PATH

internal object BytecodeHelper {

    internal fun BytecodeContext.updatePatchStatus(
        methodName: String,
        isYouTube: Boolean
    ) {
        val integrationPath =
            if (isYouTube)
                UTILS_PATH
            else
                MUSIC_UTILS_PATH

        this.classes.forEach { classDef ->
            if (classDef.type.endsWith("$integrationPath/PatchStatus;")) {
                val patchStatusMethod =
                    this.proxy(classDef).mutableClass.methods.first { it.name == methodName }

                patchStatusMethod.replaceInstruction(
                    0,
                    "const/4 v0, 0x1"
                )
            }
        }
    }
}