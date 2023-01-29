package app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.MethodFingerprintExtensions.name
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.UTILS_PATH

@Name("return-youtube-dislike-bytecode-patch")
@DependsOn(
    [
        LegacyVideoIdPatch::class,
        PlayerTypeHookPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ReturnYouTubeDislikeBytecodePatch : BytecodePatch(
    listOf(
        DislikeFingerprint,
        LikeFingerprint,
        RemoveLikeFingerprint,
        TextComponentSpecFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        listOf(
            LikeFingerprint.toPatch(Vote.LIKE),
            DislikeFingerprint.toPatch(Vote.DISLIKE),
            RemoveLikeFingerprint.toPatch(Vote.REMOVE_LIKE)
        ).forEach { (fingerprint, vote) ->
            with(fingerprint.result ?: return PatchResultError("Failed to find ${fingerprint.name} method.")) {
                mutableMethod.addInstructions(
                    0,
                    """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->sendVote(I)V
                    """
                )
            }
        }

        LegacyVideoIdPatch.injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        val createComponentResult = TextComponentSpecFingerprint.result ?: return PatchResultError("Failed to find TextComponentSpecFingerprint method.")
        val createComponentMethod = createComponentResult.mutableMethod

        val conversionContextParam = 5
        val textRefParam = createComponentMethod.parameters.size - 2

        createComponentMethod.addInstructions(
            0,
            """
            move-object/from16 v7, p$conversionContextParam
            move-object/from16 v8, p$textRefParam
            invoke-static {v7, v8}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onComponentCreated(Ljava/lang/Object;Ljava/util/concurrent/atomic/AtomicReference;)V
            """
        )

        return PatchResultSuccess()
    }
    private companion object {
        const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
            "$UTILS_PATH/ReturnYouTubeDislikePatch;"
    }

    private fun MethodFingerprint.toPatch(voteKind: Vote) = VotePatch(this, voteKind)

    private data class VotePatch(val fingerprint: MethodFingerprint, val voteKind: Vote)

    private enum class Vote(val value: Int) {
        LIKE(1),
        DISLIKE(-1),
        REMOVE_LIKE(0)
    }
}
