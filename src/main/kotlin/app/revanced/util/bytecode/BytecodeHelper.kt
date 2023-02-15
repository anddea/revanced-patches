package app.revanced.util.bytecode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.util.integrations.Constants.UTILS_PATH

internal object BytecodeHelper {

    internal fun BytecodeContext.injectInit(
        methods: String,
        descriptor: String
    ) {
        this.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (classDef.type.endsWith("WatchWhileActivity;") && method.name == "onCreate") {
                    val hookMethod =
                        this.proxy(classDef).mutableClass.methods.first { it.name == "onCreate" }

                    hookMethod.addInstruction(
                        2,
                        "invoke-static {}, $UTILS_PATH/$methods;->$descriptor()V"
                    )
                }
            }
        }
    }

    internal fun BytecodeContext.updatePatchStatus(patch: String) {
        this.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (classDef.type.endsWith("PatchStatus;") && method.name == patch) {
                    val patchStatusMethod =
                        this.proxy(classDef).mutableClass.methods.first { it.name == patch }

                    patchStatusMethod.replaceInstruction(
                        0,
                        "const/4 v0, 0x1"
                    )
                }
            }
        }
    }
}