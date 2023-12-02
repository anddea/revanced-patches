package app.revanced.patches.youtube.utils.returnyoutubedislike.general

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.playerresponse.PlayerResponsePatch
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.DislikeFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.LikeFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.RemoveLikeFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.TextComponentAtomicReferenceFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.TextComponentAtomicReferenceLegacyFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.TextComponentConstructorFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.TextComponentContextFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints.TextComponentTmpFingerprint
import app.revanced.patches.youtube.utils.returnyoutubedislike.oldlayout.ReturnYouTubeDislikeOldLayoutPatch
import app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.ReturnYouTubeDislikeRollingNumberPatch
import app.revanced.patches.youtube.utils.returnyoutubedislike.shorts.ReturnYouTubeDislikeShortsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.videoid.general.VideoIdPatch
import app.revanced.util.integrations.Constants.COMPONENTS_PATH
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Patch(
    name = "Return YouTube Dislike",
    description = "Shows the dislike count of videos using the Return YouTube Dislike API.",
    dependencies = [
        LithoFilterPatch::class,
        PlayerResponsePatch::class,
        ReturnYouTubeDislikeOldLayoutPatch::class,
        ReturnYouTubeDislikeRollingNumberPatch::class,
        ReturnYouTubeDislikeShortsPatch::class,
        SettingsPatch::class,
        VideoIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.43"
            ]
        )
    ]
)
@Suppress("unused")
object ReturnYouTubeDislikePatch : BytecodePatch(
    setOf(
        DislikeFingerprint,
        LikeFingerprint,
        RemoveLikeFingerprint,
        TextComponentConstructorFingerprint
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


        TextComponentConstructorFingerprint.result?.let { parentResult ->

            TextComponentContextFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val booleanIndex = it.scanResult.patternScanResult!!.endIndex

                    for (index in booleanIndex downTo 0) {
                        if (getInstruction(index).opcode != Opcode.IGET_OBJECT) continue

                        val targetReference =
                            getInstruction<ReferenceInstruction>(index).reference.toString()

                        if (targetReference.endsWith("Ljava/util/Map;")) {
                            conversionContextFieldReference =
                                getInstruction<ReferenceInstruction>(index - 1).reference

                            break
                        }
                    }
                }
            } ?: throw TextComponentContextFingerprint.exception

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
            } ?: throw TextComponentTmpFingerprint.exception


            val textComponentAtomicReferenceResult =
                TextComponentAtomicReferenceFingerprint.also {
                    it.resolve(context, parentResult.classDef)
                }.result
                    ?: TextComponentAtomicReferenceLegacyFingerprint.also {
                        it.resolve(context, parentResult.classDef)
                    }.result
                    ?: throw TextComponentAtomicReferenceLegacyFingerprint.exception

            TextComponentAtomicReferenceFingerprint.also {
                it.resolve(context, parentResult.classDef)
            }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val originalRegisterA =
                        getInstruction<TwoRegisterInstruction>(startIndex + 2).registerA

                    replaceInstruction(
                        startIndex + 2,
                        "move-object v$originalRegisterA, v$tmpRegister"
                    )
                    replaceInstruction(
                        startIndex + 1,
                        "move-result-object v$tmpRegister"
                    )
                }
            }

            textComponentAtomicReferenceResult.let {
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
            }
        } ?: throw TextComponentConstructorFingerprint.exception

        if (SettingsPatch.upward1834) {
            LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)
            PlayerResponsePatch.injectCall("$FILTER_CLASS_DESCRIPTOR->newPlayerResponseVideoId(Ljava/lang/String;Z)V")
        }

        PlayerResponsePatch.injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->preloadVideoId(Ljava/lang/String;Z)V")
        VideoIdPatch.injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        /**
         * Add ReVanced Extended Settings
         */
        SettingsPatch.addReVancedPreference("ryd_settings")

        SettingsPatch.updatePatchStatus("Return YouTube Dislike")

    }

    private const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
        "$UTILS_PATH/ReturnYouTubeDislikePatch;"

    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ReturnYouTubeDislikeFilterPatch;"

    private lateinit var conversionContextFieldReference: Reference
    private var tmpRegister: Int = 12

    private fun MethodFingerprint.toPatch(voteKind: Vote) = VotePatch(this, voteKind)

    private data class VotePatch(val fingerprint: MethodFingerprint, val voteKind: Vote)

    private enum class Vote(val value: Int) {
        LIKE(1),
        DISLIKE(-1),
        REMOVE_LIKE(0)
    }
}
