package app.revanced.patches.youtube.utils.recyclerview

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private lateinit var recyclerViewTreeObserverMutableMethod: MutableMethod
private var recyclerViewTreeObserverInsertIndex = 0

val bottomSheetRecyclerViewPatch = bytecodePatch(
    description = "bottomSheetRecyclerViewPatch"
) {
    execute {
        /**
         * If this value is false, OldQualityLayoutPatch and OldSpeedLayoutPatch will not work.
         * This value is usually true so this patch is not strictly necessary,
         * But in very rare cases this value may be false.
         * Therefore, we need to force this to be true.
         */
        if (bottomSheetRecyclerViewBuilderFingerprint.resolvable()) {
            bottomSheetRecyclerViewBuilderFingerprint.injectLiteralInstructionBooleanCall(
                45382015L,
                "0x1"
            )
        }

        recyclerViewTreeObserverFingerprint.methodOrThrow().apply {
            recyclerViewTreeObserverMutableMethod = this

            val onDrawListenerIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IPUT_OBJECT
                        && getReference<FieldReference>()?.type == "Landroid/view/ViewTreeObserver${'$'}OnDrawListener;"
            }
            recyclerViewTreeObserverInsertIndex =
                indexOfFirstInstructionReversedOrThrow(onDrawListenerIndex, Opcode.CHECK_CAST) + 1
        }
    }
}

fun bottomSheetRecyclerViewHook(descriptor: String) =
    recyclerViewTreeObserverMutableMethod.addInstruction(
        recyclerViewTreeObserverInsertIndex++,
        "invoke-static/range { p2 .. p2 }, $descriptor"
    )
