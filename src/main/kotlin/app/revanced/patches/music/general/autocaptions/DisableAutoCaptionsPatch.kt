package app.revanced.patches.music.general.autocaptions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.integrations.Constants.GENERAL
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.fingerprints.captions.SubtitleTrackFingerprint
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Disable auto captions",
    description = "Disables forced auto captions.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")],
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
                        invoke-static {v$register}, $GENERAL->disableAutoCaptions(Z)Z
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