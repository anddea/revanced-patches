package app.revanced.patches.youtube.video.speed

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.NewFlyoutPanelOnClickListenerFingerprint
import app.revanced.patches.youtube.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.videocpn.VideoCpnPatch
import app.revanced.patches.youtube.video.speed.fingerprints.NewPlaybackSpeedChangedFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch(
    name = "Default playback speed",
    description = "Adds ability to set default playback speed settings.",
    dependencies = [
        OverrideSpeedHookPatch::class,
        SettingsPatch::class,
        VideoCpnPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40"
            ]
        )
    ]
)
@Suppress("unused")
object PlaybackSpeedPatch : BytecodePatch(
    setOf(NewFlyoutPanelOnClickListenerFingerprint)
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

    private const val INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/PlaybackSpeedPatch;"
}