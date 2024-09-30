package app.revanced.patches.youtube.utils.flyoutmenu

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.PlaybackRateBottomSheetBuilderFingerprint
import app.revanced.patches.youtube.utils.flyoutmenu.fingerprints.VideoQualityBottomSheetClassFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.addStaticFieldToIntegration
import app.revanced.util.resultOrThrow

@Patch(
    description = "Hooks YouTube to open the playback speed or video quality flyout menu in the integration.",
    dependencies = [SharedResourceIdPatch::class]
)
object FlyoutMenuHookPatch : BytecodePatch(
    setOf(
        PlaybackRateBottomSheetBuilderFingerprint,
        VideoQualityBottomSheetClassFingerprint
    )
) {
    private const val INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    override fun execute(context: BytecodeContext) {

        PlaybackRateBottomSheetBuilderFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0}, $definingClass->$name()V
                        :ignore
                        return-void
                    """

                context.addStaticFieldToIntegration(
                    INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR,
                    "showPlaybackSpeedFlyoutMenu",
                    "playbackRateBottomSheetClass",
                    definingClass,
                    smaliInstructions
                )
            }
        }

        VideoQualityBottomSheetClassFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        const/4 v1, 0x1
                        invoke-virtual {v0, v1}, $definingClass->$name(Z)V
                        :ignore
                        return-void
                    """

                context.addStaticFieldToIntegration(
                    INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR,
                    "showVideoQualityFlyoutMenu",
                    "videoQualityBottomSheetClass",
                    definingClass,
                    smaliInstructions
                )
            }
        }
    }
}
