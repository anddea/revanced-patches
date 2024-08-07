package app.revanced.patches.youtube.player.overlaybuttons

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.video.information.VideoInformationPatch

@Patch(dependencies = [VideoInformationPatch::class])
object OverlayButtonsBytecodePatch : BytecodePatch(
    emptySet()
) {
    private const val INTEGRATIONS_ALWAYS_REPEAT_CLASS_DESCRIPTOR =
        "$UTILS_PATH/AlwaysRepeatPatch;"

    override fun execute(context: BytecodeContext) {

        // region patch for always repeat

        VideoInformationPatch.videoEndMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $INTEGRATIONS_ALWAYS_REPEAT_CLASS_DESCRIPTOR->alwaysRepeat()Z
                    move-result v0
                    if-eqz v0, :end
                    return-void
                    """, ExternalLabel("end", getInstruction(0))
            )
        }

        // endregion

    }
}
