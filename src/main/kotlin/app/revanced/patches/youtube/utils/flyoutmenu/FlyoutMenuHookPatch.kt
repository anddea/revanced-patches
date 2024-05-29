package app.revanced.patches.youtube.utils.flyoutmenu

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.flyoutmenu.fingerprints.PlaybackRateBottomSheetClassFingerprint
import app.revanced.patches.youtube.utils.flyoutmenu.fingerprints.VideoQualityBottomSheetClassFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.addFieldAndInstructions
import app.revanced.util.resultOrThrow

@Patch(
    description = "Hooks YouTube to open the playback speed or video quality flyout menu in the integration.",
    dependencies = [SharedResourceIdPatch::class]
)
object FlyoutMenuHookPatch : BytecodePatch(
    setOf(
        PlaybackRateBottomSheetClassFingerprint,
        VideoQualityBottomSheetClassFingerprint
    )
) {
    private const val INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    override fun execute(context: BytecodeContext) {

        val videoUtilsMutableClass = context.findClass(
            INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR
        )!!.mutableClass

        PlaybackRateBottomSheetClassFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val playbackRateBottomSheetBuilderMethodName =
                    it.mutableClass.methods.find { method -> method.parameters.isEmpty() && method.returnType == "V" }
                        ?.name
                        ?: throw PatchException("Could not find PlaybackRateBottomSheetBuilderMethod")

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0}, $definingClass->$playbackRateBottomSheetBuilderMethodName()V
                        :ignore
                        return-void
                    """

                videoUtilsMutableClass.addFieldAndInstructions(
                    context,
                    "showPlaybackSpeedFlyoutMenu",
                    "playbackRateBottomSheetClass",
                    definingClass,
                    smaliInstructions,
                    true
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

                videoUtilsMutableClass.addFieldAndInstructions(
                    context,
                    "showVideoQualityFlyoutMenu",
                    "videoQualityBottomSheetClass",
                    definingClass,
                    smaliInstructions,
                    true
                )
            }
        }
    }
}
