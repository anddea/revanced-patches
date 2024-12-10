package app.revanced.patches.music.utils.videotype

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.extension.Constants.UTILS_PATH
import app.revanced.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/VideoTypeHookPatch;"

@Suppress("unused")
val videoTypeHookPatch = bytecodePatch(
    description = "videoTypeHookPatch"
) {

    execute {

        videoTypeFingerprint.matchOrThrow(videoTypeParentFingerprint).let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.startIndex + 3
                val referenceIndex = insertIndex + 1
                val referenceInstruction =
                    getInstruction<ReferenceInstruction>(referenceIndex).reference

                addInstructionsWithLabels(
                    insertIndex, """
                        if-nez p0, :dismiss
                        sget-object p0, $referenceInstruction
                        :dismiss
                        invoke-static {p0}, $EXTENSION_CLASS_DESCRIPTOR->setVideoType(Ljava/lang/Enum;)V
                        """
                )
            }
        }
    }
}
