package app.revanced.patches.music.utils.flyoutmenu

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.flyoutmenu.fingerprints.PlaybackRateBottomSheetClassFingerprint
import app.revanced.patches.music.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.addFieldAndInstructions
import app.revanced.util.resultOrThrow

@Patch(
    description = "Hooks YouTube Music to open the playback speed flyout menu in the integration.",
    dependencies = [SharedResourceIdPatch::class]
)
object FlyoutMenuHookPatch : BytecodePatch(
    setOf(PlaybackRateBottomSheetClassFingerprint)
) {
    private const val INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    override fun execute(context: BytecodeContext) {

        PlaybackRateBottomSheetClassFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0}, $definingClass->$name()V
                        :ignore
                        return-void
                    """

                context.findClass(
                    INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR
                )!!.mutableClass.addFieldAndInstructions(
                    context,
                    "showPlaybackSpeedFlyoutMenu",
                    "playbackRateBottomSheetClass",
                    definingClass,
                    smaliInstructions,
                    true
                )
            }
        }
    }
}
