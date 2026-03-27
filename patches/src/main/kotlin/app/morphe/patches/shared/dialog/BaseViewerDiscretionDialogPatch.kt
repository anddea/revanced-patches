package app.morphe.patches.shared.dialog

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.utils.playservice.is_20_21_or_greater
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/youtube/patches/RemoveViewerDiscretionDialogPatch;"

fun baseViewerDiscretionDialogPatch(
    classDescriptor: String,
    isAgeVerified: Boolean = false
) = bytecodePatch(
    description = "baseViewerDiscretionDialogPatch"
) {
    execute {
        if (is_20_21_or_greater && BackgroundPlaybackManagerShortsFingerprint.matchOrNull() != null) {
            CreateDialogFingerprint.let {
                it.method.apply {
                    val showDialogIndex = it.instructionMatches.last().index
                    val dialogRegister =
                        getInstruction<FiveRegisterInstruction>(showDialogIndex).registerC

                    replaceInstructions(
                        showDialogIndex,
                        "invoke-static { v$dialogRegister }, $EXTENSION_CLASS_DESCRIPTOR->" +
                                "confirmDialog(Landroid/app/AlertDialog;)V",
                    )
                }
            }

            CreateModernDialogFingerprint.let {
                it.method.apply {
                    val showDialogIndex = it.instructionMatches.last().index
                    val dialogRegister =
                        getInstruction<FiveRegisterInstruction>(showDialogIndex).registerC

                    replaceInstructions(
                        showDialogIndex,
                        "invoke-static { v$dialogRegister }, $EXTENSION_CLASS_DESCRIPTOR->" +
                                "confirmDialog(Landroid/app/AlertDialog\$Builder;)Landroid/app/AlertDialog;",
                    )

                    val dialogStyleIndex = it.instructionMatches.first().index
                    val dialogStyleRegister =
                        getInstruction<OneRegisterInstruction>(dialogStyleIndex).registerA

                    addInstructions(
                        dialogStyleIndex + 1,
                        """
                        invoke-static { v$dialogStyleRegister }, $EXTENSION_CLASS_DESCRIPTOR->disableModernDialog(Z)Z
                        move-result v$dialogStyleRegister
                    """
                    )
                }
            }

            val PlayabilityStatusFingerprint = Fingerprint(
                accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
                returnType = "Z",
                parameters = listOf(PlayabilityStatusEnumFingerprint.originalClassDef.type),
                custom = { method, _ ->
                    // There's another similar method that's difficult to identify with a typical fingerprint.
                    // Instruction counter is used to identify the target method.
                    method.implementation!!.instructions.count() < 10
                }
            )

            PlayabilityStatusFingerprint.match(
                BackgroundPlaybackManagerShortsFingerprint.originalClassDef
            ).method.addInstruction(
                0,
                "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                        "setPlayabilityStatus(Ljava/lang/Enum;)V"
            )
        } else {
            createDialogFingerprint
                .methodOrThrow()
                .invoke(classDescriptor, "confirmDialog")

            if (isAgeVerified) {
                ageVerifiedFingerprint.matchOrThrow().let {
                    it.getWalkerMethod(it.instructionMatches.last().index - 1)
                        .invoke(classDescriptor, "confirmDialogAgeVerified")
                }
            }
        }
    }
}

private fun MutableMethod.invoke(classDescriptor: String, methodName: String) {
    val showDialogIndex = indexOfFirstInstructionOrThrow {
        getReference<MethodReference>()?.name == "show"
    }
    val dialogRegister = getInstruction<FiveRegisterInstruction>(showDialogIndex).registerC

    addInstruction(
        showDialogIndex + 1,
        "invoke-static { v$dialogRegister }, $classDescriptor->$methodName(Landroid/app/AlertDialog;)V"
    )
}
