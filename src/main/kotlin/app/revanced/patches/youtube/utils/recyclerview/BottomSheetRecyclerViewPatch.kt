package app.revanced.patches.youtube.utils.recyclerview

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.youtube.utils.recyclerview.fingerprints.BottomSheetRecyclerViewBuilderFingerprint
import app.revanced.patches.youtube.utils.recyclerview.fingerprints.RecyclerViewTreeObserverFingerprint
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

object BottomSheetRecyclerViewPatch : BytecodePatch(
    setOf(
        BottomSheetRecyclerViewBuilderFingerprint,
        RecyclerViewTreeObserverFingerprint
    )
) {
    private lateinit var recyclerViewTreeObserverResult: MethodFingerprintResult

    override fun execute(context: BytecodeContext) {

        /**
         * If this value is false, OldQualityLayoutPatch and OldSpeedLayoutPatch will not work.
         * This value is usually true so this patch is not strictly necessary,
         * But in very rare cases this value may be false.
         * Therefore, we need to force this to be true.
         */
        BottomSheetRecyclerViewBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(45382015) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        recyclerViewTreeObserverResult = RecyclerViewTreeObserverFingerprint.resultOrThrow()

    }

    fun injectCall(descriptor: String) {
        recyclerViewTreeObserverResult.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val recyclerViewRegister = 2

                addInstruction(
                    insertIndex,
                    "invoke-static/range { p$recyclerViewRegister .. p$recyclerViewRegister }, $descriptor"
                )
            }
        }

    }
}
