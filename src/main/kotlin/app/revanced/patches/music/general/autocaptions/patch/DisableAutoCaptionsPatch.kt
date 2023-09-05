package app.revanced.patches.music.general.autocaptions.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.shared.fingerprints.captions.SubtitleTrackFingerprint
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Disable auto captions")
@Description("Disables forced auto captions.")
@DependsOn([SettingsPatch::class])
@MusicCompatibility
class DisableAutoCaptionsPatch : BytecodePatch(
    listOf(SubtitleTrackFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        SubtitleTrackFingerprint.result?.let {
            it.mutableMethod.apply {
                val index = implementation!!.instructions.size - 1
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $MUSIC_GENERAL->disableAutoCaptions(Z)Z
                        move-result v$register
                        """
                )
            }
        } ?: throw SubtitleTrackFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_captions",
            "false"
        )

    }
}