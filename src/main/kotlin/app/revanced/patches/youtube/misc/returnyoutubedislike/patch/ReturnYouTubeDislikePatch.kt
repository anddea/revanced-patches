package app.revanced.patches.youtube.misc.returnyoutubedislike.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.replaceInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.returnyoutubedislike.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference

@Patch
@Name("return-youtube-dislike")
@Description("Shows the dislike count of videos using the Return YouTube Dislike API.")
@DependsOn(
    [
        MainstreamVideoIdPatch::class,
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
        ShortsTextComponentParentFingerprint,
        TextComponentConstructorFingerprint,
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


        TextComponentConstructorFingerprint.result?.let { parentResult ->
            TextComponentContextFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                with (it.mutableMethod) {
                    val conversionContextIndex = it.scanResult.patternScanResult!!.startIndex
                    val conversionContextRegister =
                        (instruction(conversionContextIndex) as TwoRegisterInstruction).registerA

                    val instructions = implementation!!.instructions
                    val targetName = "a"
                    val targetType = "Ljava/util/concurrent/atomic/AtomicReference;"

                    for ((index, instruction) in instructions.withIndex()) {
                        if (instruction.opcode != Opcode.IGET_OBJECT) continue

                        val indexReference = (instruction as ReferenceInstruction).reference as FieldReference
                        if (indexReference.name != targetName || indexReference.type != targetType) continue
                        atomicReferenceRegister = (instruction(index) as TwoRegisterInstruction).registerA
                        insertIndex = index + 1
                        break
                    }

                    if (insertIndex == 0) return TextComponentContextFingerprint.toErrorResult()

                    addInstruction(
                        insertIndex,
                        "invoke-static {v$conversionContextRegister, v$atomicReferenceRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onComponentCreated(Ljava/lang/Object;Ljava/util/concurrent/atomic/AtomicReference;)V"
                    )
                }
            } ?: return TextComponentContextFingerprint.toErrorResult()
        } ?: return TextComponentConstructorFingerprint.toErrorResult()


        TextComponentSpecFingerprint.result?.let {
            with(it.mutableMethod) {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val charSequenceRegister = (instruction(endIndex) as OneRegisterInstruction).registerA
                val contextTempRegister = (instruction(startIndex) as FiveRegisterInstruction).registerD

                replaceInstructions(endIndex, "move-object/from16 v$contextTempRegister, p0")
                addInstructions(
                    endIndex + 1, """
                        invoke-static {v$contextTempRegister, v$charSequenceRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onComponentCreated(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$charSequenceRegister
                        return-object v$charSequenceRegister
                    """
                )
            }
        } ?: return TextComponentSpecFingerprint.toErrorResult()

        ShortsTextComponentParentFingerprint.result?.let {
            with (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
            ) {
                val insertInstructions = this.implementation!!.instructions
                val insertIndex = insertInstructions.size - 1
                val insertRegister = (insertInstructions.elementAt(insertIndex) as OneRegisterInstruction).registerA

                this.insertShorts(insertIndex, insertRegister)
            }

            with (it.mutableMethod) {
                val insertInstructions = this.implementation!!.instructions
                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 2
                val insertRegister = (insertInstructions.elementAt(insertIndex - 1) as OneRegisterInstruction).registerA

                this.insertShorts(insertIndex, insertRegister)
            }
        } ?: return ShortsTextComponentParentFingerprint.toErrorResult()


        MainstreamVideoIdPatch.injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        /*
         * Add ReVanced Settings
         */
        SettingsPatch.addReVancedPreference("ryd_settings")

        SettingsPatch.updatePatchStatus("return-youtube-dislike")

        return PatchResultSuccess()
    }
    private companion object {
        const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
            "$UTILS_PATH/ReturnYouTubeDislikePatch;"

        var atomicReferenceRegister: Int = 0
        var insertIndex: Int = 0
    }

    private fun MethodFingerprint.toPatch(voteKind: Vote) = VotePatch(this, voteKind)

    private data class VotePatch(val fingerprint: MethodFingerprint, val voteKind: Vote)

    private enum class Vote(val value: Int) {
        LIKE(1),
        DISLIKE(-1),
        REMOVE_LIKE(0)
    }

    private fun MutableMethod.insertShorts(index: Int, register: Int) {
        addInstructions(
            index, """
                invoke-static {v$register}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onShortsComponentCreated(Landroid/text/Spanned;)Landroid/text/Spanned;
                move-result-object v$register
                """
        )
    }
}
