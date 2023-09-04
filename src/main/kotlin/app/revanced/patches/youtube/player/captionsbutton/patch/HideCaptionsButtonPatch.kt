package app.revanced.patches.youtube.player.captionsbutton.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.SubtitleButtonControllerFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Hide captions button")
@Description("Hides the captions button in the video player.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class HideCaptionsButtonBytecodePatch : BytecodePatch(
    listOf(SubtitleButtonControllerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        SubtitleButtonControllerFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.IGET_OBJECT
                }
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.IGET_BOOLEAN
                } + 1

                addInstruction(
                    insertIndex,
                    "invoke-static {v$targetRegister}, $PLAYER->hideCaptionsButton(Landroid/widget/ImageView;)V"
                )
            }
        } ?: throw SubtitleButtonControllerFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_CAPTIONS_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-captions-button")

    }
}