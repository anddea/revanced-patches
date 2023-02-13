package app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("return-youtube-dislike-bytecode-patch")
@DependsOn(
    [
        MainstreamVideoIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ReturnYouTubeDislikeBytecodePatch : BytecodePatch(
    listOf(
        DislikeFingerprint,
        LikeFingerprint,
        RemoveLikeFingerprint,
        ShortsTextComponentParentFingerprint,
        TextComponentSpecFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        listOf(
            LikeFingerprint.toPatch(Vote.LIKE),
            DislikeFingerprint.toPatch(Vote.DISLIKE),
            RemoveLikeFingerprint.toPatch(Vote.REMOVE_LIKE)
        ).forEach { (fingerprint, vote) ->
            with(fingerprint.result ?: return fingerprint.toErrorResult()) {
                mutableMethod.addInstructions(
                    0,
                    """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->sendVote(I)V
                    """
                )
            }
        }

        ShortsTextComponentParentFingerprint.result?.let {
            with(context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
            ) {
                val insertInstructions = this.implementation!!.instructions
                val insertIndex = insertInstructions.size - 1
                val insertRegister = (insertInstructions.elementAt(insertIndex) as OneRegisterInstruction).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, ${INTEGRATIONS_RYD_CLASS_DESCRIPTOR}->onShortsComponentCreated(Landroid/text/Spanned;)Landroid/text/Spanned;
                        move-result-object v$insertRegister
                    """
                )
            }

        } ?: return ShortsTextComponentParentFingerprint.toErrorResult()

        MainstreamVideoIdPatch.injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

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
