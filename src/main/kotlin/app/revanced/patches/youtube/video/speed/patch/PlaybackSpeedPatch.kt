package app.revanced.patches.youtube.video.speed.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.NewFlyoutPanelOnClickListenerFingerprint
import app.revanced.patches.youtube.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.videocpn.patch.VideoCpnPatch
import app.revanced.patches.youtube.video.speed.fingerprints.NewPlaybackSpeedChangedFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch
@Name("Default playback speed")
@Description("Adds ability to set default playback speed settings.")
@DependsOn(
    [
        OverrideSpeedHookPatch::class,
        SettingsPatch::class,
        VideoCpnPatch::class
    ]
)
@YouTubeCompatibility
class PlaybackSpeedPatch : BytecodePatch(
    listOf(NewFlyoutPanelOnClickListenerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        NewFlyoutPanelOnClickListenerFingerprint.result?.let { parentResult ->
            NewPlaybackSpeedChangedFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let { result ->
                arrayOf(result, OverrideSpeedHookPatch.playbackSpeedChangedResult).forEach {
                    it.mutableMethod.apply {
                        val index = it.scanResult.patternScanResult!!.endIndex
                        val register = getInstruction<FiveRegisterInstruction>(index).registerD

                        addInstruction(
                            index,
                            "invoke-static {v$register}, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->userChangedSpeed(F)V"
                        )
                    }
                }
            } ?: throw NewPlaybackSpeedChangedFingerprint.exception
        } ?: throw NewFlyoutPanelOnClickListenerFingerprint.exception

        VideoCpnPatch.injectCall("$INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Z)V")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: DEFAULT_PLAYBACK_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("default-playback-speed")

    }

    private companion object {
        const val INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/PlaybackSpeedPatch;"
    }
}