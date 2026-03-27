package app.morphe.patches.youtube.utils.recyclerview

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private lateinit var recyclerViewTreeObserverMutableMethod: MutableMethod
private var recyclerViewTreeObserverInsertIndex = 0

val recyclerViewTreeObserverPatch = bytecodePatch(
    description = "recyclerViewTreeObserverPatch"
) {
    execute {
        /**
         * If this value is false, RecyclerViewTreeObserver is not initialized.
         * This value is usually true so this patch is not strictly necessary,
         * But in very rare cases this value may be false.
         * Therefore, we need to force this to be true.
         */
        recyclerViewBuilderFingerprint.injectLiteralInstructionBooleanCall(
            RECYCLER_VIEW_BUILDER_FEATURE_FLAG,
            "0x1"
        )

        recyclerViewTreeObserverFingerprint.methodOrThrow().apply {
            recyclerViewTreeObserverMutableMethod = this

            val onDrawListenerIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IPUT_OBJECT &&
                        getReference<FieldReference>()?.type == "Landroid/view/ViewTreeObserver${'$'}OnDrawListener;"
            }
            recyclerViewTreeObserverInsertIndex =
                indexOfFirstInstructionReversedOrThrow(onDrawListenerIndex, Opcode.CHECK_CAST) + 1
        }
    }
}

fun recyclerViewTreeObserverHook(descriptor: String) =
    recyclerViewTreeObserverMutableMethod.addInstruction(
        recyclerViewTreeObserverInsertIndex++,
        "invoke-static/range { p2 .. p2 }, $descriptor"
    )
