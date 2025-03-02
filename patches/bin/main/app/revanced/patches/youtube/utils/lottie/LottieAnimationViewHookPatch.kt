package app.revanced.patches.youtube.utils.lottie

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.methodOrThrow

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/LottieAnimationViewPatch;"

val lottieAnimationViewHookPatch = bytecodePatch(
    description = "lottieAnimationViewHookPatch",
) {
    execute {

        findMethodOrThrow(EXTENSION_CLASS_DESCRIPTOR) {
            name == "setAnimation"
        }.addInstruction(
            0,
            "invoke-virtual {p0, p1}, " +
                    LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR +
                    "->" +
                    setAnimationFingerprint.methodOrThrow().name +
                    "(I)V"
        )

    }
}
