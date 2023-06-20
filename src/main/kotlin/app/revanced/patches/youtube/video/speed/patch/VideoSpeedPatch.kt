package app.revanced.patches.youtube.video.speed.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.video.speed.fingerprints.OrganicPlaybackContextModelFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch
@Name("default-video-speed")
@Description("Adds ability to set default video speed settings.")
@DependsOn(
    [
        OverrideSpeedHookPatch::class,
        PlayerTypeHookPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class VideoSpeedPatch : BytecodePatch(
    listOf(OrganicPlaybackContextModelFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        OverrideSpeedHookPatch.videoSpeedChangedResult.let {
            it.mutableMethod.apply {
                val index = it.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<FiveRegisterInstruction>(index).registerD

                addInstruction(
                    index,
                    "invoke-static {v$register}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->userChangedSpeed(F)V"
                )
            }
        }

        OrganicPlaybackContextModelFingerprint.result?.mutableMethod?.addInstruction(
            2,
            "invoke-static {p1,p2}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Z)V"
        ) ?: return OrganicPlaybackContextModelFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: DEFAULT_VIDEO_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("default-video-speed")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoSpeedPatch;"
    }
}