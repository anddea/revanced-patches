package app.morphe.patches.music.utils.flyoutmenu

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.extension.Constants.EXTENSION_PATH
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.playbackRateBottomSheetClassFingerprint
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.util.addStaticFieldToExtension
import app.morphe.util.fingerprint.methodOrThrow

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

val flyoutMenuHookPatch = bytecodePatch(
    description = "flyoutMenuHookPatch",
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
    )

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
