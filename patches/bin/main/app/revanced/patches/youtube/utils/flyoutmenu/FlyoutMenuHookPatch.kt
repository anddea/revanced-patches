package app.revanced.patches.youtube.utils.flyoutmenu

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.youtube.utils.playbackRateBottomSheetBuilderFingerprint
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.fingerprint.methodOrThrow

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

val flyoutMenuHookPatch = bytecodePatch(
    description = "flyoutMenuHookPatch",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(sharedResourceIdPatch)

    execute {
        playbackRateBottomSheetBuilderFingerprint.methodOrThrow().apply {
            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    invoke-virtual {v0}, $definingClass->$name()V
                    :ignore
                    return-void
                    """

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "showPlaybackSpeedFlyoutMenu",
                "playbackRateBottomSheetClass",
                definingClass,
                smaliInstructions
            )
        }

        videoQualityBottomSheetClassFingerprint.methodOrThrow().apply {
            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    const/4 v1, 0x1
                    invoke-virtual {v0, v1}, $definingClass->$name(Z)V
                    :ignore
                    return-void
                    """

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "showVideoQualityFlyoutMenu",
                "videoQualityBottomSheetClass",
                definingClass,
                smaliInstructions
            )
        }
    }
}
