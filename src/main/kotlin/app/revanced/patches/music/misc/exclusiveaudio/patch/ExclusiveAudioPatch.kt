package app.revanced.patches.music.misc.exclusiveaudio.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.exclusiveaudio.fingerprints.AudioOnlyEnablerFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fix.decoding.patch.DecodingPatch

@Patch
@Name("Exclusive audio playback")
@Description("Enables the option to play music without video.")
@DependsOn([DecodingPatch::class])
@MusicCompatibility
class ExclusiveAudioPatch : BytecodePatch(
    listOf(AudioOnlyEnablerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        AudioOnlyEnablerFingerprint.result?.mutableMethod?.let {
            it.replaceInstruction(it.implementation!!.instructions.count() - 1, "const/4 v0, 0x1")
            it.addInstruction("return v0")
        } ?: throw AudioOnlyEnablerFingerprint.exception

    }
}
