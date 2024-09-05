package app.revanced.patches.music.general.autocaptions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.fingerprints.SubtitleTrackFingerprint
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object AutoCaptionsPatch : BaseBytecodePatch(
    name = "Disable auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(SubtitleTrackFingerprint),
) {
    override fun execute(context: BytecodeContext) {

        SubtitleTrackFingerprint.resultOrThrow().mutableMethod.apply {
            val index = implementation!!.instructions.lastIndex
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index, """
                    invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->disableAutoCaptions(Z)Z
                    move-result v$register
                    """
            )
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_captions",
            "false"
        )

    }
}