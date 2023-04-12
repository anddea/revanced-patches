package app.revanced.patches.youtube.misc.returnyoutubedislike.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
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
import app.revanced.patches.youtube.misc.videoid.patch.VideoIdPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction3rc
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("return-youtube-dislike")
@Description("Shows the dislike count of videos using the Return YouTube Dislike API.")
@DependsOn(
    [
        SettingsPatch::class,
        VideoIdPatch::class
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
        TextComponentSpecExtensionFingerprint,
        TextComponentSpecParentFingerprint
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


        TextComponentSpecParentFingerprint.result?.let { parentResult ->

            TextComponentSpecObjectFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                with (it.mutableMethod) {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val endIndex = it.scanResult.patternScanResult!!.endIndex

                    val dummyRegister = (instruction(startIndex) as BuilderInstruction3rc).registerCount +
                            (instruction(startIndex) as BuilderInstruction3rc).startRegister - 1
                    val targetRegister = (instruction(endIndex) as Instruction35c).registerC

                    val instructions = implementation!!.instructions
                    val targetString =
                        ((instructions.elementAt(startIndex) as ReferenceInstruction).reference as MethodReference).parameterTypes.first().toString()

                    for ((index, instruction) in instructions.withIndex()) {
                        if (instruction.opcode != Opcode.IGET_OBJECT) continue

                        val indexReference = (instruction as ReferenceInstruction).reference as FieldReference

                        if (indexReference.type != targetString) continue
                        objectReference = indexReference
                        objectIndex = index
                        break
                    }

                    if (objectIndex == 0) return TextComponentSpecObjectFingerprint.toErrorResult()

                    val conversionContextParam = 0

                    addInstructions(
                        endIndex + 1, """
                            move-object/from16 v$dummyRegister, p$conversionContextParam
                            iget-object v$dummyRegister, v$dummyRegister, ${definingClass}->${objectReference.name}:${objectReference.type}
                            invoke-static {v$dummyRegister, v$targetRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onComponentCreated(Ljava/lang/Object;Ljava/util/concurrent/atomic/AtomicReference;)V
                        """
                    )
                }
            } ?: return TextComponentSpecObjectFingerprint.toErrorResult()


            TextComponentSpecFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                with (it.mutableMethod) {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val targetRegister = (instruction(insertIndex) as TwoRegisterInstruction).registerA

                    addInstructions(
                        insertIndex, """
                            iget-object p0, p0, ${definingClass}->${objectReference.name}:${objectReference.type}
                            invoke-static {p0, v$targetRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onComponentCreated(Ljava/lang/Object;Ljava/util/concurrent/atomic/AtomicReference;)V
                        """
                    )
                }
            } ?: return TextComponentSpecFingerprint.toErrorResult()
        } ?: return TextComponentSpecParentFingerprint.toErrorResult()


        TextComponentSpecExtensionFingerprint.result?.let {
            with (it.mutableMethod) {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val targetRegister =
                    (instruction(targetIndex) as OneRegisterInstruction).registerA
                val dummyRegister =
                    (instruction(targetIndex + 1) as OneRegisterInstruction).registerA

                addInstructions(
                    targetIndex + 1, """
                        move-object/from16 v$dummyRegister, p0
                        invoke-static {v$dummyRegister, v$targetRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->overrideLikeDislikeSpan(Ljava/lang/Object;Landroid/text/SpannableString;)Landroid/text/SpannableString;
                        move-result-object v$targetRegister
                        """
                )
            }
        } ?: return TextComponentSpecExtensionFingerprint.toErrorResult()


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


        VideoIdPatch.injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

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

        private lateinit var objectReference: FieldReference
        var objectIndex: Int = 0
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
