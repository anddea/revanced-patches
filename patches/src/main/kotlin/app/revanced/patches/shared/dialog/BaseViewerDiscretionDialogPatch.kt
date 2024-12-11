package app.revanced.patches.shared.dialog

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

fun baseViewerDiscretionDialogPatch(
    classDescriptor: String,
    isAgeVerified: Boolean = false
) = bytecodePatch(
    description = "baseViewerDiscretionDialogPatch"
) {
    execute {
        createDialogFingerprint
            .methodOrThrow()
            .invoke(classDescriptor, "confirmDialog")

        if (isAgeVerified) {
            ageVerifiedFingerprint.matchOrThrow().let {
                it.getWalkerMethod(it.patternMatch!!.endIndex - 1)
                    .invoke(classDescriptor, "confirmDialogAgeVerified")
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

