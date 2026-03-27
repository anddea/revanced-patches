package app.morphe.patches.music.utils.dismiss

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.extension.Constants.EXTENSION_PATH
import app.morphe.util.addStaticFieldToExtension
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getWalkerMethod

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

@Suppress("unused")
val dismissQueueHookPatch = bytecodePatch(
    description = "dismissQueueHookPatch"
) {

    execute {

        dismissQueueFingerprint.methodOrThrow().apply {
            val dismissQueueIndex = indexOfDismissQueueInstruction(this)

            getWalkerMethod(dismissQueueIndex).apply {
                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0}, $definingClass->$name()V
                        :ignore
                        return-void
                        """

                addStaticFieldToExtension(
                    EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                    "dismissQueue",
                    "dismissQueueClass",
                    definingClass,
                    smaliInstructions
                )
            }
        }

    }
}
