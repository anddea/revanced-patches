package app.revanced.patches.youtube.video.speed.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.NewFlyoutPanelOnClickListenerFingerprint
import app.revanced.patches.youtube.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.videocpn.patch.VideoCpnPatch
import app.revanced.patches.youtube.video.speed.fingerprints.NewVideoSpeedChangedFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch
@Name("Default video speed")
@Description("Adds ability to set default video speed settings.")
@DependsOn(
    [
        OverrideSpeedHookPatch::class,
        VideoCpnPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class VideoSpeedPatch : BytecodePatch(
    listOf(NewFlyoutPanelOnClickListenerFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        NewFlyoutPanelOnClickListenerFingerprint.result?.let { parentResult ->
            NewVideoSpeedChangedFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let { result ->
                arrayOf(result, OverrideSpeedHookPatch.videoSpeedChangedResult).forEach {
                    it.mutableMethod.apply {
                        val index = it.scanResult.patternScanResult!!.endIndex
                        val register = getInstruction<FiveRegisterInstruction>(index).registerD

                        addInstruction(
                            index,
                            "invoke-static {v$register}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->userChangedSpeed(F)V"
                        )
                    }
                }
            } ?: return NewVideoSpeedChangedFingerprint.toErrorResult()
        } ?: return NewFlyoutPanelOnClickListenerFingerprint.toErrorResult()

        VideoCpnPatch.injectCall("$INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Z)V")

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