package app.revanced.patches.youtube.utils.recyclerview

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.recyclerview.fingerprints.BottomSheetRecyclerViewBuilderFingerprint
import app.revanced.patches.youtube.utils.recyclerview.fingerprints.RecyclerViewTreeObserverFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.injectLiteralInstructionBooleanCall
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

object BottomSheetRecyclerViewPatch : BytecodePatch(
    setOf(
        BottomSheetRecyclerViewBuilderFingerprint,
        RecyclerViewTreeObserverFingerprint
    )
) {
    private lateinit var recyclerViewTreeObserverMutableMethod: MutableMethod

    private var recyclerViewTreeObserverInsertIndex = 0

    override fun execute(context: BytecodeContext) {

        /**
         * If this value is false, OldQualityLayoutPatch and OldSpeedLayoutPatch will not work.
         * This value is usually true so this patch is not strictly necessary,
         * But in very rare cases this value may be false.
         * Therefore, we need to force this to be true.
         */
        BottomSheetRecyclerViewBuilderFingerprint.result?.let {
            BottomSheetRecyclerViewBuilderFingerprint.injectLiteralInstructionBooleanCall(
                45382015,
                "0x1"
            )
        }

        RecyclerViewTreeObserverFingerprint.resultOrThrow().mutableMethod.apply {
            recyclerViewTreeObserverMutableMethod = this

            val onDrawListenerIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IPUT_OBJECT
                        && getReference<FieldReference>()?.type == "Landroid/view/ViewTreeObserver${'$'}OnDrawListener;"
            }
            recyclerViewTreeObserverInsertIndex =
                indexOfFirstInstructionReversedOrThrow(onDrawListenerIndex, Opcode.CHECK_CAST) + 1
        }

    }

    internal fun injectCall(descriptor: String) =
        recyclerViewTreeObserverMutableMethod.addInstruction(
            recyclerViewTreeObserverInsertIndex++,
            "invoke-static/range { p2 .. p2 }, $descriptor"
        )
}
