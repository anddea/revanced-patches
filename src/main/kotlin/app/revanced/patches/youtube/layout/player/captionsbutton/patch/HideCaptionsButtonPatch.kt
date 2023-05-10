package app.revanced.patches.youtube.layout.player.captionsbutton.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.SubtitleButtonControllerFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import org.jf.dexlib2.Opcode

@Patch
@Name("hide-captions-button")
@Description("Hides the captions button in the video player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HideCaptionsButtonBytecodePatch : BytecodePatch(
    listOf(SubtitleButtonControllerFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SubtitleButtonControllerFingerprint.result?.mutableMethod?.let {
            it.implementation!!.instructions.apply {
                for ((index, instruction) in this.withIndex()) {
                    if (instruction.opcode != Opcode.IGET_BOOLEAN) continue

                    it.addInstruction(
                        index + 1,
                        "invoke-static {v0}, $PLAYER->hideCaptionsButton(Landroid/widget/ImageView;)V"
                    )

                    break
                }
            }
        } ?: return SubtitleButtonControllerFingerprint.toErrorResult()

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

        return PatchResultSuccess()
    }
}