package app.revanced.patches.music.video.playback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.information.VideoInformationPatch
import app.revanced.patches.music.video.playback.fingerprints.PlaybackSpeedBottomSheetFingerprint
import app.revanced.patches.music.video.playback.fingerprints.UserQualityChangeFingerprint
import app.revanced.patches.music.video.videoid.VideoIdPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object VideoPlaybackPatch : BaseBytecodePatch(
    name = "Video playback",
    description = "Adds options to customize settings related to video playback," +
            "such as default video quality and playback speed.",
    dependencies = setOf(
        CustomPlaybackSpeedPatch::class,
        SettingsPatch::class,
        VideoIdPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        PlaybackSpeedBottomSheetFingerprint,
        UserQualityChangeFingerprint
    )
) {
    private const val INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/PlaybackSpeedPatch;"
    private const val INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/VideoQualityPatch;"

    override fun execute(context: BytecodeContext) {

        // region patch for default playback speed

        PlaybackSpeedBottomSheetFingerprint.resultOrThrow().let {
            val onItemClickMethod =
                it.mutableClass.methods.find { method -> method.name == "onItemClick" }

            onItemClickMethod?.apply {
                val targetIndex = getTargetIndex(Opcode.IGET)
                val targetRegister =
                    getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->userSelectedPlaybackSpeed(F)V"
                )
            } ?: throw PatchException("Failed to find onItemClick method")
        }

        VideoInformationPatch.playbackSpeedResult.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val speedRegister =
                    getInstruction<OneRegisterInstruction>(startIndex + 1).registerA

                addInstructions(
                    startIndex + 2, """
                        invoke-static {v$speedRegister}, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeed(F)F
                        move-result v$speedRegister
                        """
                )
            }
        }

        // endregion

        // region patch for default video quality

        UserQualityChangeFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val qualityChangedClass =
                    context.findClass(
                        (getInstruction<BuilderInstruction21c>(endIndex))
                            .reference.toString()
                    )!!
                        .mutableClass

                val onItemClickMethod =
                    qualityChangedClass.methods.find { method -> method.name == "onItemClick" }

                onItemClickMethod?.addInstruction(
                    0,
                    "invoke-static {}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userSelectedVideoQuality()V"
                ) ?: throw PatchException("Failed to find onItemClick method")
            }
        }

        VideoIdPatch.hookVideoId("$INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        // endregion

        SettingsPatch.addPreferenceWithIntent(
            CategoryType.VIDEO,
            "revanced_custom_playback_speeds"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_remember_playback_speed_last_selected",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_remember_video_quality_last_selected",
            "true"
        )
    }
}
