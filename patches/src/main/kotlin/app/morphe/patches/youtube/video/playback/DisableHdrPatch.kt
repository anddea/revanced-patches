package app.morphe.patches.youtube.video.playback

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.transformation.IMethodCall
import app.morphe.patches.all.misc.transformation.fromMethodReference
import app.morphe.patches.all.misc.transformation.transformInstructionsPatch
import app.morphe.patches.youtube.utils.extension.Constants.VIDEO_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/HDRVideoPatch;"

/**
 * For versions lower than 19.20, there are three matching methods.
 * For versions higher than 19.20, there is only one matching method.
 *
 * TODO: If support for YouTube 19.16 is dropped in the future,
 *       implement this as a regular fingerprint, not a 'transformInstructionsPatch'
 */
val disableHdrPatch = bytecodePatch(
    description = "disableHdrPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        transformInstructionsPatch(
            filterMap = filter@{ _, _, instruction, instructionIndex ->
                val reference = instruction.getReference<MethodReference>() ?: return@filter null
                if (fromMethodReference<MethodCall>(reference) == null) return@filter null

                instruction to instructionIndex
            },
            transform = { method, entry ->
                val (instruction, index) = entry
                val register = (instruction as FiveRegisterInstruction).registerC

                method.replaceInstruction(
                    index,
                    "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->" +
                            "disableHDRVideo(Landroid/view/Display\$HdrCapabilities;)[I",
                )
            },
        ),
    )
}

private enum class MethodCall(
    override val definedClassName: String,
    override val methodName: String,
    override val methodParams: Array<String>,
    override val returnType: String,
) : IMethodCall {
    SupportedHdrTypes(
        "Landroid/view/Display\$HdrCapabilities;",
        "getSupportedHdrTypes",
        emptyArray(),
        "[I"
    ),
}