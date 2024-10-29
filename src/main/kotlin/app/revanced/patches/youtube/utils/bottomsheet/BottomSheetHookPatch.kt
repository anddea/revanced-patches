package app.revanced.patches.youtube.utils.bottomsheet

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.bottomsheet.fingerprint.BottomSheetBehaviorFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.findMethodOrThrow
import app.revanced.util.resultOrThrow

@Patch(dependencies = [SharedResourceIdPatch::class])
object BottomSheetHookPatch : BytecodePatch(
    setOf(BottomSheetBehaviorFingerprint)
) {
    private const val INTEGRATIONS_BOTTOM_SHEET_HOOK_CLASS_DESCRIPTOR =
        "$UTILS_PATH/BottomSheetHookPatch;"

    override fun execute(context: BytecodeContext) {

        val bottomSheetClass =
            BottomSheetBehaviorFingerprint.resultOrThrow().mutableMethod.definingClass

        arrayOf(
            "onAttachedToWindow",
            "onDetachedFromWindow"
        ).forEach { methodName ->
            context.findMethodOrThrow(bottomSheetClass) {
                name == methodName
            }.addInstruction(
                1,
                "invoke-static {}, $INTEGRATIONS_BOTTOM_SHEET_HOOK_CLASS_DESCRIPTOR->$methodName()V"
            )
        }
    }
}
