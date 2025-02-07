package app.revanced.patches.youtube.utils.request

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private lateinit var buildRequestMethod: MutableMethod
private var urlRegister = 0
private var mapRegister = 0
private var offSet = 0

val buildRequestPatch = bytecodePatch(
    description = "buildRequestPatch",
) {
    dependsOn(sharedExtensionPatch)

    execute {
        buildRequestFingerprint.methodOrThrow().apply {
            buildRequestMethod = this

            val newRequestBuilderIndex = indexOfNewUrlRequestBuilderInstruction(this)
            urlRegister =
                getInstruction<FiveRegisterInstruction>(newRequestBuilderIndex).registerD

            val entrySetIndex = indexOfEntrySetInstruction(this)
            val isLegacyTarget = entrySetIndex < 0
            mapRegister = if (isLegacyTarget)
                urlRegister + 1
            else
                getInstruction<FiveRegisterInstruction>(entrySetIndex).registerC

            if (isLegacyTarget) {
                addInstructions(
                    newRequestBuilderIndex + 2,
                    "move-object/from16 v$mapRegister, p1"
                )
                offSet++
            }
        }
    }
}

internal fun hookBuildRequest(descriptor: String) {
    buildRequestMethod.apply {
        val insertIndex = indexOfNewUrlRequestBuilderInstruction(this) + 2 + offSet

        addInstructions(
            insertIndex,
            "invoke-static { v$urlRegister, v$mapRegister }, $descriptor"
        )
    }
}
