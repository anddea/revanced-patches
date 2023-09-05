package app.revanced.util.bytecode

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH
import app.revanced.util.integrations.Constants.UTILS_PATH

internal object BytecodeHelper {

    internal fun BytecodeContext.injectInit(
        methods: String,
        descriptor: String,
        isYouTube: Boolean
    ) {
        val activityClass =
            if (isYouTube)
                "/WatchWhileActivity;"
            else
                "/MusicActivity;"

        val integrationPath =
            if (isYouTube)
                UTILS_PATH
            else
                MUSIC_UTILS_PATH

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (classDef.type.endsWith(activityClass) && method.name == "onCreate") {
                    val hookMethod =
                        this.proxy(classDef).mutableClass.methods.first { it.name == "onCreate" }

                    hookMethod.addInstruction(
                        2,
                        "invoke-static/range {p0 .. p0}, $integrationPath/$methods;->$descriptor(Landroid/content/Context;)V"
                    )
                }
            }
        }
    }

    internal fun BytecodeContext.updatePatchStatus(patch: String) {
        this.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (classDef.type.endsWith("/PatchStatus;") && method.name == patch) {
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