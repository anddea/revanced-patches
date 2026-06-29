/*
 * Copyright (C) 2022-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.youtube.utils.returnyoutubedislike

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.textcomponent.hookSpannableString
import app.morphe.patches.shared.textcomponent.hookTextComponent
import app.morphe.patches.shared.textcomponent.textComponentPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.componentlist.hookElementList
import app.morphe.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
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
import app.morphe.patches.youtube.player.action.restoreOldVideoActionBarPatch
import app.morphe.util.findFreeRegister
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
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

        RollingNumberSetterFingerprint.let {
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
        RollingNumberMeasureAnimatedTextFingerprint.let {
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

        val rollingNumberMeasureTextClass = RollingNumberMeasureTextParentFingerprint.match().classDef
        RollingNumberMeasureStaticLabelFingerprint.match(rollingNumberMeasureTextClass).let {
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
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        returnYouTubeDislikeRollingNumberPatch,
        returnYouTubeDislikeShortsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        restoreOldVideoActionBarPatch,
        videoInformationPatch,
        lazilyConvertedElementHookPatch,
    )

    execute {
        // Hook the endpoint parser instead of three endpoint methods. Playlist actions use the
        // same endpoint names but omit the video ID, so passing both values lets the extension
        // reject them without generating incorrect RYD requests.
        val endpointServiceNameField = EndpointServiceNameFingerprint
            .instructionMatches.last().instruction.getReference<FieldReference>()!!
        val likeEndpointParserClass = DislikeFingerprint.classDef.superclass!!
        val videoIdField = requestParameterCheckFingerprint(likeEndpointParserClass)
            .instructionMatches.last().instruction.getReference<FieldReference>()!!

        likeEndpointParserFingerprint(likeEndpointParserClass).let {
            it.method.apply {
                val insertIndex = it.instructionMatches[1].index + 1
                val likeEndpointTargetClassRegister =
                    getInstruction<TwoRegisterInstruction>(insertIndex - 1).registerA
                val registerProvider = getFreeRegisterProvider(
                    insertIndex,
                    2,
                    likeEndpointTargetClassRegister,
                )
                val endpointServiceNameRegister = registerProvider.getFreeRegister()
                val videoIdRegister = registerProvider.getFreeRegister()

                addInstructions(
                    insertIndex,
                    """
                        iget-object v$endpointServiceNameRegister, p0, $endpointServiceNameField
                        iget-object v$videoIdRegister, v$likeEndpointTargetClassRegister, $videoIdField
                        invoke-static {v$endpointServiceNameRegister, v$videoIdRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->sendVote(Ljava/lang/String;Ljava/lang/String;)V
                    """,
                )
            }
        }

        hookTextComponent(EXTENSION_RYD_CLASS_DESCRIPTOR)
        hookElementList("$EXTENSION_RYD_CLASS_DESCRIPTOR->onLazilyConvertedElementLoaded")

        // region Inject newVideoLoaded event handler to update dislikes when a new video is loaded
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
            TextComponentFeatureFlagFingerprint.method.apply {
                val literalIndex = indexOfFirstLiteralInstruction(LITHO_NEW_TEXT_COMPONENT_FEATURE_FLAG)
                val moveResultIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
                val register = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA
                addInstructions(
                    moveResultIndex + 1,
                    "const/16 v$register, 0x0",
                )
            }
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
