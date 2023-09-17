package app.revanced.patches.music.video.speed.patch

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
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.music.video.speed.fingerprints.PlaybackSpeedBottomSheetFingerprint
import app.revanced.patches.music.video.speed.fingerprints.PlaybackSpeedBottomSheetParentFingerprint
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_VIDEO_PATH
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch
@Name("Remember playback speed")
@Description("Save the playback speed value whenever you change the playback speed.")
@DependsOn(
    [
        OverrideSpeedHookPatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
class PlaybackSpeedPatch : BytecodePatch(
    listOf(PlaybackSpeedBottomSheetParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PlaybackSpeedBottomSheetParentFingerprint.result?.let { parentResult ->
            PlaybackSpeedBottomSheetFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex
                    val targetRegister =
                        getInstruction<FiveRegisterInstruction>(targetIndex).registerD

                    addInstruction(
                        targetIndex,
                        "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->userChangedSpeed(F)V"
                    )
                }
            } ?: throw PlaybackSpeedBottomSheetFingerprint.exception
        } ?: throw PlaybackSpeedBottomSheetParentFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.VIDEO,
            "revanced_enable_save_playback_speed",
            "false"
        )

    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$MUSIC_VIDEO_PATH/PlaybackSpeedPatch;"
    }
}