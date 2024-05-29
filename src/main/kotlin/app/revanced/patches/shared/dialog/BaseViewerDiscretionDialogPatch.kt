package app.revanced.patches.shared.dialog

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.dialog.fingerprints.CreateDialogFingerprint
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getWalkerMethod
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

abstract class BaseViewerDiscretionDialogPatch(
    private val classDescriptor: String,
    private val additionalFingerprints: Set<MethodFingerprint> = emptySet()
) : BytecodePatch(
    buildSet {
        add(CreateDialogFingerprint)
        additionalFingerprints.let(::addAll)
    }
) {
    private fun MutableMethod.invoke(isAgeVerified: Boolean) {
        val showDialogIndex = getTargetIndexWithMethodReferenceName("show")
        val dialogRegister = getInstruction<FiveRegisterInstruction>(showDialogIndex).registerC

        val methodName =
            if (isAgeVerified)
                "confirmDialogAgeVerified"
            else
                "confirmDialog"

        addInstruction(
            showDialogIndex + 1,
            "invoke-static { v$dialogRegister }, $classDescriptor->$methodName(Landroid/app/AlertDialog;)V"
        )
    }

    override fun execute(context: BytecodeContext) {
        CreateDialogFingerprint.resultOrThrow().mutableMethod.invoke(false)

        if (additionalFingerprints.isNotEmpty()) {
            additionalFingerprints.forEach { fingerprint ->
                fingerprint.resultOrThrow().let {
                    it.getWalkerMethod(context, it.scanResult.patternScanResult!!.endIndex - 1)
                        .invoke(true)
                }
            }
        }

    }
}