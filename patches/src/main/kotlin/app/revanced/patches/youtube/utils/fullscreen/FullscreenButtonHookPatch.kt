package app.revanced.patches.youtube.utils.fullscreen

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.playservice.is_20_02_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

internal var enterFullscreenMethods = mutableListOf<MutableMethod>()

val fullscreenButtonHookPatch = bytecodePatch(
    description = "fullscreenButtonHookPatch"
) {

    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    execute {
        fun getParameters(): Pair<MutableMethod, String> {
            nextGenWatchLayoutFullscreenModeFingerprint.methodOrThrow().apply {
                val methodIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_DIRECT &&
                            getReference<MethodReference>()?.parameterTypes?.size == 2
                }
                val fieldIndex =
                    indexOfFirstInstructionReversedOrThrow(methodIndex, Opcode.IGET_OBJECT)
                val fullscreenActionClass =
                    (getInstruction<ReferenceInstruction>(fieldIndex).reference as FieldReference).type

                if (is_20_02_or_greater) {
                    val setAnimatorListenerIndex =
                        indexOfFirstInstructionOrThrow(methodIndex, Opcode.INVOKE_VIRTUAL)
                    getWalkerMethod(setAnimatorListenerIndex).apply {
                        val addListenerIndex = indexOfFirstInstructionOrThrow {
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    getReference<MethodReference>()?.name == "addListener"
                        }
                        val animatorListenerAdapterClass = getInstruction<ReferenceInstruction>(
                            indexOfFirstInstructionReversedOrThrow(
                                addListenerIndex,
                                Opcode.NEW_INSTANCE
                            )
                        ).reference.toString()
                        return Pair(
                            findMethodOrThrow(animatorListenerAdapterClass) { parameters.isEmpty() },
                            fullscreenActionClass
                        )
                    }
                } else {
                    val animatorListenerClass =
                        (getInstruction<ReferenceInstruction>(methodIndex).reference as MethodReference).definingClass
                    return Pair(
                        findMethodOrThrow(animatorListenerClass) { parameters == listOf("I") },
                        fullscreenActionClass
                    )
                }
            }
        }

        val (animatorListenerMethod, fullscreenActionClass) = getParameters()

        val (enterFullscreenReference, exitFullscreenReference, opcodeName) =
            with(animatorListenerMethod) {
                val enterFullscreenIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    reference?.returnType == "V" &&
                            reference.definingClass == fullscreenActionClass &&
                            reference.parameterTypes.isEmpty()
                }
                val exitFullscreenIndex = indexOfFirstInstructionReversedOrThrow {
                    val reference = getReference<MethodReference>()
                    reference?.returnType == "V" &&
                            reference.definingClass == fullscreenActionClass &&
                            reference.parameterTypes.isEmpty()
                }

                val enterFullscreenReference =
                    getInstruction<ReferenceInstruction>(enterFullscreenIndex).reference
                val exitFullscreenReference =
                    getInstruction<ReferenceInstruction>(exitFullscreenIndex).reference
                val opcode = getInstruction(enterFullscreenIndex).opcode

                val enterFullscreenClass =
                    (enterFullscreenReference as MethodReference).definingClass

                if (opcode == Opcode.INVOKE_INTERFACE) {
                    classes.forEach { classDef ->
                        if (enterFullscreenMethods.size >= 2)
                            return@forEach
                        if (!classDef.interfaces.contains(enterFullscreenClass))
                            return@forEach

                        val enterFullscreenMethod =
                            proxy(classDef)
                                .mutableClass
                                .methods
                                .find { method -> method.name == enterFullscreenReference.name }
                                ?: throw PatchException("No matching classes: $enterFullscreenClass")

                        enterFullscreenMethods.add(enterFullscreenMethod)
                    }
                } else {
                    val enterFullscreenMethod =
                        findMethodOrThrow(enterFullscreenClass) {
                            name == enterFullscreenReference.name
                        }
                    enterFullscreenMethods.add(enterFullscreenMethod)
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
