package app.revanced.patches.youtube.layout.player.captionsbutton.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.fingerprints.SubtitleButtonControllerFingerprint
import app.revanced.shared.util.integrations.Constants.PLAYER_LAYOUT
import org.jf.dexlib2.Opcode

@Name("hide-captions-button-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideCaptionsButtonBytecodePatch : BytecodePatch(listOf(
    SubtitleButtonControllerFingerprint,
)) {
    override fun execute(context: BytecodeContext): PatchResult {

        val subtitleButtonControllerMethod = SubtitleButtonControllerFingerprint.result!!.mutableMethod
        val subtitleButtonControllerMethodInstructions = subtitleButtonControllerMethod.implementation!!.instructions

        for ((index, instruction) in subtitleButtonControllerMethodInstructions.withIndex()) {
            if (instruction.opcode != Opcode.IGET_BOOLEAN) continue

            subtitleButtonControllerMethod.addInstruction(
                index + 1,
                "invoke-static {v0}, $PLAYER_LAYOUT->hideCaptionsButton(Landroid/widget/ImageView;)V"
            )

            break
        }

        return PatchResultSuccess()
    }
}