package app.revanced.patches.youtube.video.speed.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.overridespeed.bytecode.fingerprints.VideoSpeedSettingsFingerprint
import app.revanced.patches.youtube.misc.overridespeed.bytecode.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.patches.youtube.video.livestream.patch.LiveStreamPatch
import app.revanced.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

@Name("default-video-speed-bytecode-patch")
@DependsOn(
    [
        LiveStreamPatch::class,
        MainstreamVideoIdPatch::class,
        OverrideSpeedHookPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class VideoSpeedBytecodePatch : BytecodePatch(
    listOf(
        VideoSpeedSettingsFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        with(OverrideSpeedHookPatch.videoSpeedChangedResult) {
            val index = scanResult.patternScanResult!!.endIndex
            val register =
                (method.implementation!!.instructions.elementAt(index) as FiveRegisterInstruction).registerD

            mutableMethod.addInstruction(
                index,
                "invoke-static { v$register }, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR" +
                        "->" +
                        "userChangedSpeed(F)V"
            )
        }

        VideoSpeedSettingsFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "invoke-static {}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->setDefaultSpeed()V"
        ) ?: return VideoSpeedSettingsFingerprint.toErrorResult()

        MainstreamVideoIdPatch.onCreateHook(INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR, "newVideoStarted")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoSpeedPatch;"
    }
}