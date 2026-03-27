package app.morphe.patches.youtube.utils.returnyoutubedislike

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.dislikeFingerprint
import app.morphe.patches.shared.likeFingerprint
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.removeLikeFingerprint
import app.morphe.patches.shared.textcomponent.hookSpannableString
import app.morphe.patches.shared.textcomponent.hookTextComponent
import app.morphe.patches.shared.textcomponent.textComponentPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.RETURN_YOUTUBE_DISLIKE
import app.morphe.patches.youtube.utils.playservice.is_18_34_or_greater
import app.morphe.patches.youtube.utils.playservice.is_18_49_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_07_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.rollingNumberTextViewAnimationUpdateFingerprint
import app.morphe.patches.youtube.utils.rollingNumberTextViewFingerprint
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.hookShortsVideoInformation
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.morphe.patches.youtube.video.videoid.hookVideoId
import app.morphe.util.findFreeRegister
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_RYD_CLASS_DESCRIPTOR =
    "$UTILS_PATH/ReturnYouTubeDislikePatch;"

private val returnYouTubeDislikeRollingNumberPatch = bytecodePatch(
    description = "returnYouTubeDislikeRollingNumberPatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (!is_18_49_or_greater) {
            return@execute
        }

        rollingNumberSetterFingerprint.matchOrThrow().let {
            it.method.apply {
                val rollingNumberClassIndex = it.instructionMatches.first().index
                val rollingNumberClassReference =
                    getInstruction<ReferenceInstruction>(rollingNumberClassIndex).reference.toString()
                val rollingNumberConstructorMethod =
                    findMethodOrThrow(rollingNumberClassReference)
                val charSequenceFieldReference = with(rollingNumberConstructorMethod) {
                    getInstruction<ReferenceInstruction>(
                        indexOfFirstInstructionOrThrow(Opcode.IPUT_OBJECT)
                    ).reference
                }

                val insertIndex = rollingNumberClassIndex + 1
                val charSequenceInstanceRegister =
                    getInstruction<OneRegisterInstruction>(rollingNumberClassIndex).registerA

                val conversionContextRegister = implementation!!.registerCount - parameters.size + 1
                val freeRegister = findFreeRegister(
                    insertIndex,
                    charSequenceInstanceRegister,
                    conversionContextRegister
                )

                addInstructions(
                    insertIndex, """
                        iget-object v$freeRegister, v$charSequenceInstanceRegister, $charSequenceFieldReference
                        invoke-static {v$conversionContextRegister, v$freeRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->onRollingNumberLoaded(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$freeRegister
                        iput-object v$freeRegister, v$charSequenceInstanceRegister, $charSequenceFieldReference
                        """
                )
            }
        }

        // Rolling Number text views use the measured width of the raw string for layout.
        // Modify the measure text calculation to include the left drawable separator if needed.
        rollingNumberMeasureAnimatedTextFingerprint.matchOrThrow().let {
            it.method.apply {
                val endIndex = it.instructionMatches.last().index
                val measuredTextWidthIndex = endIndex - 2
                val measuredTextWidthRegister =
                    getInstruction<TwoRegisterInstruction>(measuredTextWidthIndex).registerA

                addInstructions(
                    endIndex + 1, """
                        invoke-static {p1, v$measuredTextWidthRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->onRollingNumberMeasured(Ljava/lang/String;F)F
                        move-result v$measuredTextWidthRegister
                        """
                )

                val ifGeIndex = indexOfFirstInstructionOrThrow(Opcode.IF_GE)
                val ifGeInstruction = getInstruction<TwoRegisterInstruction>(ifGeIndex)

                removeInstruction(ifGeIndex)
                addInstructionsWithLabels(
                    ifGeIndex, """
                        if-ge v${ifGeInstruction.registerA}, v${ifGeInstruction.registerB}, :jump
                        """, ExternalLabel("jump", getInstruction(endIndex))
                )
            }
        }

        rollingNumberMeasureStaticLabelFingerprint.matchOrThrow(
            rollingNumberMeasureTextParentFingerprint
        ).let {
            it.method.apply {
                val measureTextIndex = it.instructionMatches.first().index + 1
                val freeRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                addInstructions(
                    measureTextIndex + 1, """
                        move-result v$freeRegister
                        invoke-static {p1, v$freeRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->onRollingNumberMeasured(Ljava/lang/String;F)F
                        """
                )
            }
        }

        // The rolling number Span is missing styling since it's initially set as a String.
        // Modify the UI text view and use the styled like/dislike Span.
        arrayOf(
            // Initial TextView is set in this method.
            rollingNumberTextViewFingerprint
                .methodOrThrow(),

            // Video less than 24 hours after uploaded, like counts will be updated in real time.
            // Whenever like counts are updated, TextView is set in this method.
            rollingNumberTextViewAnimationUpdateFingerprint
                .methodOrThrow(rollingNumberTextViewFingerprint)
        ).forEach { method ->
            method.apply {
                val setTextIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "setText"
                }
                val textViewRegister =
                    getInstruction<FiveRegisterInstruction>(setTextIndex).registerC
                val textSpanRegister =
                    getInstruction<FiveRegisterInstruction>(setTextIndex).registerD

                addInstructions(
                    setTextIndex, """
                        invoke-static {v$textViewRegister, v$textSpanRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->updateRollingNumber(Landroid/widget/TextView;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$textSpanRegister
                        """
                )
            }
        }
    }
}

private val returnYouTubeDislikeShortsPatch = bytecodePatch(
    description = "returnYouTubeDislikeShortsPatch"
) {
    dependsOn(
        textComponentPatch,
        versionCheckPatch
    )

    execute {
        shortsTextViewFingerprint.matchOrThrow().let {
            it.method.apply {
                val startIndex = it.instructionMatches.first().index

                val isDisLikesBooleanIndex =
                    indexOfFirstInstructionReversedOrThrow(startIndex, Opcode.IGET_BOOLEAN)
                val textViewFieldIndex =
                    indexOfFirstInstructionReversedOrThrow(startIndex, Opcode.IGET_OBJECT)

                // If the field is true, the TextView is for a dislike button.
                val isDisLikesBooleanReference =
                    getInstruction<ReferenceInstruction>(isDisLikesBooleanIndex).reference

                val textViewFieldReference = // Like/Dislike button TextView field
                    getInstruction<ReferenceInstruction>(textViewFieldIndex).reference

                // Check if the hooked TextView object is that of the dislike button.
                // If RYD is disabled, or the TextView object is not that of the dislike button, the execution flow is not interrupted.
                // Otherwise, the TextView object is modified, and the execution flow is interrupted to prevent it from being changed afterward.
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.CHECK_CAST) + 1

                addInstructionsWithLabels(
                    insertIndex, """
                    # Check, if the TextView is for a dislike button
                    iget-boolean v0, p0, $isDisLikesBooleanReference
                    if-eqz v0, :ryd_disabled
                    
                    # Hook the TextView, if it is for the dislike button
                    iget-object v0, p0, $textViewFieldReference
                    invoke-static {v0}, $EXTENSION_RYD_CLASS_DESCRIPTOR->setShortsDislikes(Landroid/view/View;)Z
                    move-result v0
                    if-eqz v0, :ryd_disabled
                    return-void
                    """, ExternalLabel("ryd_disabled", getInstruction(insertIndex))
                )
            }
        }

        if (is_18_34_or_greater) {
            hookSpannableString(
                EXTENSION_RYD_CLASS_DESCRIPTOR,
                "onCharSequenceLoaded"
            )
        }
    }
}

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ReturnYouTubeDislikeFilterPatch;"

@Suppress("unused")
val returnYouTubeDislikePatch = bytecodePatch(
    RETURN_YOUTUBE_DISLIKE.title,
    RETURN_YOUTUBE_DISLIKE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        returnYouTubeDislikeRollingNumberPatch,
        returnYouTubeDislikeShortsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        videoInformationPatch,
    )

    execute {
        mapOf(
            likeFingerprint to Vote.LIKE,
            dislikeFingerprint to Vote.DISLIKE,
            removeLikeFingerprint to Vote.REMOVE_LIKE,
        ).forEach { (fingerprint, vote) ->
            fingerprint.methodOrThrow().addInstructions(
                0,
                """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $EXTENSION_RYD_CLASS_DESCRIPTOR->sendVote(I)V
                    """,
            )
        }

        hookTextComponent(EXTENSION_RYD_CLASS_DESCRIPTOR)

        // region Inject newVideoLoaded event handler to update dislikes when a new video is loaded.
        hookVideoId("$EXTENSION_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        // Hook the player response video id, to start loading RYD sooner in the background.
        hookPlayerResponseVideoId("$EXTENSION_RYD_CLASS_DESCRIPTOR->preloadVideoId(Ljava/lang/String;Z)V")

        // endregion

        // Player response video id is needed to search for the video ids in Shorts litho components.
        if (is_18_34_or_greater) {
            addLithoFilter(FILTER_CLASS_DESCRIPTOR)
            hookPlayerResponseVideoId("$FILTER_CLASS_DESCRIPTOR->newPlayerResponseVideoId(Ljava/lang/String;Z)V")
            hookShortsVideoInformation("$FILTER_CLASS_DESCRIPTOR->newShortsVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        }

        // endregion

        if (is_20_07_or_greater) {
            // Turn off a/b flag that enables new code for creating litho spans.
            // If enabled then the litho text span hook is never called.
            // Target code is very obfuscated and exactly what the code does is not clear.
            // Return late so debug patch logs if the flag is enabled.
            textComponentFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                LITHO_NEW_TEXT_COMPONENT_FEATURE_FLAG,
                "0x0"
            )
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: RETURN_YOUTUBE_DISLIKE"
            ),
            RETURN_YOUTUBE_DISLIKE
        )

        // endregion
    }
}

enum class Vote(val value: Int) {
    LIKE(1),
    DISLIKE(-1),
    REMOVE_LIKE(0),
}
