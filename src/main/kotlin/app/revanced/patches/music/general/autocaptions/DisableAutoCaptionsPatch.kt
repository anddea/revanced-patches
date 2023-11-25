package app.revanced.patches.music.general.autocaptions

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.fingerprints.captions.SubtitleTrackFingerprint
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Disable auto captions",
    description = "Disables forced auto captions.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ],
)
@Suppress("unused")
object DisableAutoCaptionsPatch : BytecodePatch(
    setOf(SubtitleTrackFingerprint)
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