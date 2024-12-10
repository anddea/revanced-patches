package app.revanced.patches.youtube.utils.bottomsheet

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.definingClassOrThrow

private const val EXTENSION_BOTTOM_SHEET_HOOK_CLASS_DESCRIPTOR =
    "$UTILS_PATH/BottomSheetHookPatch;"

val bottomSheetHookPatch = bytecodePatch(
    description = "bottomSheetHookPatch"
) {
    execute {
        val bottomSheetClass =
            bottomSheetBehaviorFingerprint.definingClassOrThrow()

        arrayOf(
            "onAttachedToWindow",
            "onDetachedFromWindow"
        ).forEach { methodName ->
            findMethodOrThrow(bottomSheetClass) {
                name == methodName
            }.addInstruction(
                1,
                "invoke-static {}, $EXTENSION_BOTTOM_SHEET_HOOK_CLASS_DESCRIPTOR->$methodName()V"
            )
        }
    }
}
