package app.revanced.patches.youtube.utils.fullscreen

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

internal lateinit var enterFullscreenMethod: MutableMethod

val fullscreenButtonHookPatch = bytecodePatch(
    description = "fullscreenButtonHookPatch"
) {

    dependsOn(sharedExtensionPatch)

    execute {
        val (referenceClass, fullscreenActionClass) = with (nextGenWatchLayoutFullscreenModeFingerprint.methodOrThrow()) {
            val targetIndex = indexOfFirstInstructionReversedOrThrow {
                opcode == Opcode.INVOKE_DIRECT &&
                        getReference<MethodReference>()?.parameterTypes?.size == 2
            }
            val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference as MethodReference

            Pair(targetReference.definingClass, targetReference.parameterTypes[1].toString())
        }

        val (enterFullscreenReference, exitFullscreenReference, opcodeName) =
            with (findMethodOrThrow(referenceClass) { parameters == listOf("I") }) {
                val enterFullscreenIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    reference?.returnType == "V" &&
                            reference.definingClass == fullscreenActionClass &&
                            reference.parameterTypes.size == 0
                }
                val exitFullscreenIndex = indexOfFirstInstructionReversedOrThrow {
                    val reference = getReference<MethodReference>()
                    reference?.returnType == "V" &&
                            reference.definingClass == fullscreenActionClass &&
                            reference.parameterTypes.size == 0
                }

                val enterFullscreenReference =
                    getInstruction<ReferenceInstruction>(enterFullscreenIndex).reference
                val exitFullscreenReference =
                    getInstruction<ReferenceInstruction>(exitFullscreenIndex).reference
                val opcode = getInstruction(enterFullscreenIndex).opcode

                val enterFullscreenClass = (enterFullscreenReference as MethodReference).definingClass

                enterFullscreenMethod = if (opcode == Opcode.INVOKE_INTERFACE) {
                    classes.find { classDef -> classDef.interfaces.contains(enterFullscreenClass) }
                        ?.let { classDef ->
                            proxy(classDef)
                                .mutableClass
                                .methods
                                .find { method -> method.name == enterFullscreenReference.name }
                        } ?: throw PatchException("No matching classes: $enterFullscreenClass")
                } else {
                    findMethodOrThrow(enterFullscreenClass) {
                        name == enterFullscreenReference.name
                    }
                }

                Triple(
                    enterFullscreenReference,
                    exitFullscreenReference,
                    opcode.name
                )
            }

        nextGenWatchLayoutConstructorFingerprint.methodOrThrow().apply {
            val targetIndex = indexOfFirstInstructionReversedOrThrow {
                opcode == Opcode.CHECK_CAST &&
                        getReference<TypeReference>()?.type == fullscreenActionClass
            }
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "sput-object v$targetRegister, $EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR->fullscreenActionClass:$fullscreenActionClass"
            )

            val enterFullscreenModeSmaliInstructions =
                """
                    if-eqz v0, :ignore
                    $opcodeName {v0}, $enterFullscreenReference
                    :ignore
                    return-void
                    """

            val exitFullscreenModeSmaliInstructions =
                """
                    if-eqz v0, :ignore
                    $opcodeName {v0}, $exitFullscreenReference
                    :ignore
                    return-void
                    """

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "enterFullscreenMode",
                "fullscreenActionClass",
                fullscreenActionClass,
                enterFullscreenModeSmaliInstructions,
                false
            )

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "exitFullscreenMode",
                "fullscreenActionClass",
                fullscreenActionClass,
                exitFullscreenModeSmaliInstructions,
                false
            )
        }
    }
}
