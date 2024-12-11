package app.revanced.patches.music.utils.flyoutmenu

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.fingerprint.methodOrThrow

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

val flyoutMenuHookPatch = bytecodePatch(
    description = "flyoutMenuHookPatch",
) {
    dependsOn(sharedResourceIdPatch)

    execute {

        playbackRateBottomSheetClassFingerprint.methodOrThrow().apply {
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

    }
}
