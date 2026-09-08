/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - COOLak (https://github.com/COOLak)
 * - Jav1x (https://github.com/Jav1x)
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

package app.morphe.patches.youtube.video.voiceovertranslation

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.spoof.CreateStreamingDataFingerprint
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.youtube.utils.auth.authHookPatch
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.VOICE_OVER_TRANSLATION
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.player.overlaybuttons.overlayButtonsPatch
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.video.information.hookVideoInformation
import app.morphe.patches.youtube.video.information.onCreateHook
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.information.videoTimeHook
import app.morphe.patches.youtube.video.videoid.hookVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import app.morphe.util.updatePatchStatus

private const val EXTENSION_VOT_PATH =
    "$EXTENSION_PATH/patches/voiceovertranslation"

private const val EXTENSION_VOT_CLASS_DESCRIPTOR =
    "$EXTENSION_VOT_PATH/VoiceOverTranslationPatch;"

private const val EXTENSION_GOOGLE_VOT_CLASS_DESCRIPTOR =
    "$EXTENSION_VOT_PATH/GoogleVoiceOverTranslationPatch;"

val voiceOverTranslationBytecodePatch = bytecodePatch(
    description = "voiceOverTranslationBytecodePatch"
) {
    dependsOn(
        videoInformationPatch,
        videoIdPatch,
        playerTypeHookPatch,
        fixProtoLibraryPatch,
        authHookPatch,
    )

    execute {
        // Read the final native fields after optional stream spoofing has completed.
        // The response's VideoDetails identifies the source even during preloading.
        CreateStreamingDataFingerprint.let {
            val streamField = it.instructionMatches[1].instruction.getReference<FieldReference>()!!
            val detailsField = it.instructionMatches[5].instruction.getReference<FieldReference>()!!
            val helper = ImmutableMethod(
                it.classDef.type,
                "patch_cacheVotAudioSources",
                emptyList(),
                "V",
                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        iget-object v0, p0, $streamField
                        iget-object v1, p0, $detailsField
                        invoke-static { v0, v1 }, $EXTENSION_VOT_CLASS_DESCRIPTOR->cacheAudioSources(Ljava/lang/Object;Ljava/lang/Object;)V
                        return-void
                    """
                )
            }
            it.classDef.methods.add(helper)
            it.classDef.methods.filter { method -> method.name == "<init>" }.forEach { method ->
                method.apply {
                    findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
                        addInstruction(index, "invoke-direct/range { p0 .. p0 }, $helper")
                    }
                }
            }
        }

        // Hook video time updates for audio sync
        videoTimeHook(
            EXTENSION_VOT_CLASS_DESCRIPTOR,
            "setVideoTime"
        )

        // Hook player initialization (Yandex)
        onCreateHook(
            EXTENSION_VOT_CLASS_DESCRIPTOR,
            "initialize"
        )

        // Hook new video started event to trigger translation (Yandex)
        hookVideoInformation(
            "$EXTENSION_VOT_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V"
        )

        // Update the patch status to enabled for the extension (Yandex)
        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "VoiceOverTranslation")

        // Hook video time updates for TTS synchronization (Google)
        videoTimeHook(
            EXTENSION_GOOGLE_VOT_CLASS_DESCRIPTOR,
            "videoTimeChanged"
        )

        // Hook new video loaded event to load transcript (Google)
        hookVideoId("$EXTENSION_GOOGLE_VOT_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        // Update the patch status to enabled for the extension (Google)
        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "GoogleVoiceOverTranslation")
    }
}

@Suppress("unused")
val voiceOverTranslationPatch = resourcePatch(
    VOICE_OVER_TRANSLATION.title,
    VOICE_OVER_TRANSLATION.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        overlayButtonsPatch,
        voiceOverTranslationBytecodePatch,
        votOriginalVolumeBytecodePatch,
        settingsPatch,
    )

    execute {
        /**
         * Add settings
         */
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: VOICE_OVER_TRANSLATION"
            ),
            VOICE_OVER_TRANSLATION
        )
    }
}
