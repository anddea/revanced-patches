package app.revanced.patches.youtube.utils.returnyoutubedislike.general.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.DislikeFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.LikeFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.RemoveLikeFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.TextComponentAtomicReferenceFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.TextComponentConstructorFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.TextComponentContextFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.TextComponentTmpFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.oldlayout.patch.ReturnYouTubeDislikeOldLayoutPatch
import app.revanced.patches.youtube.utils.returnyoutubedislike.shorts.patch.ReturnYouTubeDislikeShortsPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.Reference

@Patch
@Name("return-youtube-dislike")
@Description("Shows the dislike count of videos using the Return YouTube Dislike API.")
@DependsOn(
    [
        MainstreamVideoIdPatch::class,
        ReturnYouTubeDislikeOldLayoutPatch::class,
        ReturnYouTubeDislikeShortsPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ReturnYouTubeDislikePatch : BytecodePatch(
    listOf(
        DislikeFingerprint,
        LikeFingerprint,
        RemoveLikeFingerprint,
        TextComponentConstructorFingerprint
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


        TextComponentConstructorFingerprint.result?.let { parentResult ->

            TextComponentContextFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val conversionContextIndex = it.scanResult.patternScanResult!!.startIndex
                    conversionContextFieldReference =
                        getInstruction<ReferenceInstruction>(conversionContextIndex).reference
                }
            } ?: return TextComponentContextFingerprint.toErrorResult()

            TextComponentTmpFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    tmpRegister =
                        getInstruction<FiveRegisterInstruction>(startIndex).registerE
                }
            } ?: return TextComponentTmpFingerprint.toErrorResult()


            TextComponentAtomicReferenceFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val atomicReferenceStartIndex = it.scanResult.patternScanResult!!.startIndex
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val moveCharSequenceInstruction =
                        getInstruction<TwoRegisterInstruction>(insertIndex)

                    val atomicReferenceRegister =
                        getInstruction<FiveRegisterInstruction>(atomicReferenceStartIndex).registerC

                    val charSequenceRegister =
                        moveCharSequenceInstruction.registerB

                    addInstructions(
                        insertIndex + 1, """
                            move-object/from16 v$tmpRegister, p0
                            iget-object v$tmpRegister, v$tmpRegister, $conversionContextFieldReference
                            invoke-static {v$tmpRegister, v$atomicReferenceRegister, v$charSequenceRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onLithoTextLoaded(Ljava/lang/Object;Ljava/util/concurrent/atomic/AtomicReference;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                            move-result-object v$charSequenceRegister
                            move-object v${moveCharSequenceInstruction.registerA}, v${charSequenceRegister}
                            """
                    )
                    removeInstruction(insertIndex)
                }
            } ?: return TextComponentAtomicReferenceFingerprint.toErrorResult()
        } ?: return TextComponentConstructorFingerprint.toErrorResult()


        MainstreamVideoIdPatch.injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        /**
         * Add ReVanced Settings
         */
        SettingsPatch.addReVancedPreference("ryd_settings")

        SettingsPatch.updatePatchStatus("return-youtube-dislike")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
            "$UTILS_PATH/ReturnYouTubeDislikePatch;"

        lateinit var conversionContextFieldReference: Reference
        var tmpRegister: Int = 12
    }

    private fun MethodFingerprint.toPatch(voteKind: Vote) = VotePatch(this, voteKind)

    private data class VotePatch(val fingerprint: MethodFingerprint, val voteKind: Vote)

    private enum class Vote(val value: Int) {
        LIKE(1),
        DISLIKE(-1),
        REMOVE_LIKE(0)
    }
}
