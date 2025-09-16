package app.revanced.patches.youtube.utils.request

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.util.findFreeRegister
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private lateinit var buildRequestMethod: MutableMethod
private var builderIndex = 0
private var urlRegister = 0
private var freeRegister = 0

val buildRequestPatch = bytecodePatch(
    description = "buildRequestPatch",
) {
    dependsOn(sharedExtensionPatch)

    execute {
        buildRequestMethod = buildRequestFingerprint.method.apply {
            builderIndex = indexOfNewUrlRequestBuilderInstruction(this)
            urlRegister = getInstruction<FiveRegisterInstruction>(builderIndex).registerD
            freeRegister = findFreeRegister(builderIndex, urlRegister)

            if (!getInstruction(builderIndex).toString().contains("move-object v$freeRegister, p1"))
                addInstruction(builderIndex, "move-object v$freeRegister, p1")
        }
    }
}

internal fun hookBuildRequest(descriptor: String) {
    buildRequestMethod.apply {
        addInstruction(builderIndex + 1, "invoke-static { v$urlRegister, v$freeRegister }, $descriptor")
    }
}

internal fun hookBuildRequestUrl(descriptor: String) {
    buildRequestMethod.apply {
        val insertIndex = indexOfNewUrlRequestBuilderInstruction(this)

        addInstructions(
            insertIndex, """
                invoke-static { v$urlRegister }, $descriptor
                move-result-object v$urlRegister
                """
        )
    }
}
