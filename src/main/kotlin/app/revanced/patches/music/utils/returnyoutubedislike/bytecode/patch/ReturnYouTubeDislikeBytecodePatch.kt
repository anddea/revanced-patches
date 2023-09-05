package app.revanced.patches.music.utils.returnyoutubedislike.bytecode.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.returnyoutubedislike.bytecode.fingerprints.DislikeFingerprint
import app.revanced.patches.music.utils.returnyoutubedislike.bytecode.fingerprints.LikeFingerprint
import app.revanced.patches.music.utils.returnyoutubedislike.bytecode.fingerprints.RemoveLikeFingerprint
import app.revanced.patches.music.utils.returnyoutubedislike.bytecode.fingerprints.TextComponentFingerprint
import app.revanced.patches.music.utils.videoid.patch.VideoIdPatch
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@DependsOn(
    [
        SharedResourceIdPatch::class,
        VideoIdPatch::class
    ]
)
class ReturnYouTubeDislikeBytecodePatch : BytecodePatch(
    listOf(
        DislikeFingerprint,
        LikeFingerprint,
        RemoveLikeFingerprint,
        TextComponentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        listOf(
            LikeFingerprint.toPatch(Vote.LIKE),
            DislikeFingerprint.toPatch(Vote.DISLIKE),
            RemoveLikeFingerprint.toPatch(Vote.REMOVE_LIKE)
        ).forEach { (fingerprint, vote) ->
            with(fingerprint.result ?: throw fingerprint.exception) {
                mutableMethod.addInstructions(
                    0,
                    """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->sendVote(I)V
                    """
                )
            }
        }

        TextComponentFingerprint.result?.let {
            it.mutableMethod.apply {
                var insertIndex = -1
                for ((index, instruction) in implementation!!.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.INVOKE_STATIC) continue

                    val reference = getInstruction<Instruction35c>(index).reference.toString()
                    if (!reference.endsWith("Ljava/lang/CharSequence;") && !reference.endsWith("Landroid/text/Spanned;")) continue

                    val insertRegister = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    insertIndex = index + 2

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onComponentCreated(Landroid/text/Spanned;)Landroid/text/Spanned;
                            move-result-object v$insertRegister
                            """
                    )

                    break
                }
                if (insertIndex == -1)
                    throw PatchException("target Instruction not found!")
            }
        } ?: throw TextComponentFingerprint.exception

        VideoIdPatch.injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

    }

    private companion object {
        const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
            "$MUSIC_UTILS_PATH/ReturnYouTubeDislikePatch;"
    }

    private fun MethodFingerprint.toPatch(voteKind: Vote) = VotePatch(this, voteKind)

    private data class VotePatch(val fingerprint: MethodFingerprint, val voteKind: Vote)

    private enum class Vote(val value: Int) {
        LIKE(1),
        DISLIKE(-1),
        REMOVE_LIKE(0)
    }
}
