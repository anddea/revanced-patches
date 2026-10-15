/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3566
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.general.autocaptions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.patch.PatchList.PREFERRED_CAPTION_LANGUAGE
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.onCreateHook
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/PreferredCaptionLanguagePatch;"
private const val EXTENSION_SUBTITLE_MANAGER_INTERFACE =
    $$"Lapp/morphe/extension/youtube/patches/PreferredCaptionLanguagePatch$SubtitleManagerInterface;"
private const val EXTENSION_CAPTION_TRACK_INTERFACE =
    $$"Lapp/morphe/extension/youtube/patches/PreferredCaptionLanguagePatch$CaptionTrackInterface;"

/**
 * Adds a public final method with no parameters.
 */
private fun MutableClass.addHelperMethod(name: String, returnType: String, smali: String) {
    methods.add(
        ImmutableMethod(
            type,
            name,
            listOf(),
            returnType,
            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
            null,
            null,
            MutableMethodImplementation(2),
        ).toMutable().apply {
            addInstructions(0, smali)
        }
    )
}

@Suppress("unused")
val preferredCaptionLanguagePatch = bytecodePatch(
    PREFERRED_CAPTION_LANGUAGE.title,
    PREFERRED_CAPTION_LANGUAGE.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        videoInformationPatch
    )

    execute {
        addPreference(
            arrayOf("PREFERENCE_SCREEN: GENERAL", "SETTINGS: PREFERRED_CAPTION_LANGUAGE"),
            PREFERRED_CAPTION_LANGUAGE
        )

        // Add interfaces and helper methods to allow extension code to call obfuscated code.

        CaptionTrackIsDisableOptionFingerprint.classDef.apply {
            /**
             * Field access or getter call of the matched instruction.
             */
            fun getterSmali(fingerprint: Fingerprint, matchIndex: Int): String {
                val instruction = fingerprint.instructionMatches[matchIndex].instruction
                return when (val reference = instruction.getReference<Reference>()) {
                    is FieldReference -> "iget-object v0, p0, $reference"
                    is MethodReference -> """
                        invoke-virtual { p0 }, $reference
                        move-result-object v0
                    """
                    else -> throw PatchException("Unexpected reference: $reference")
                }
            }

            interfaces.add(EXTENSION_CAPTION_TRACK_INTERFACE)

            addHelperMethod(
                "patch_getLanguageCode",
                "Ljava/lang/String;",
                """
                    ${getterSmali(CaptionTrackIsDisableOptionFingerprint, 1)}
                    return-object v0
                """
            )

            addHelperMethod(
                "patch_getVssId",
                "Ljava/lang/String;",
                """
                    ${getterSmali(CaptionTrackIsAutoTranslatedFingerprint, 0)}
                    return-object v0
                """
            )
        }

        DefaultCaptionTrackFingerprint.classDef.apply {
            val tracksManagerType = CaptionTracksManagerDirectTracksFingerprint.classDef.type
            val tracksManagerField = fields.single { field ->
                field.type == tracksManagerType
            }

            interfaces.add(EXTENSION_SUBTITLE_MANAGER_INTERFACE)

            fun addTracksGetter(name: String, tracksFingerprint: Fingerprint) {
                addHelperMethod(
                    name,
                    "Ljava/util/List;",
                    """
                        iget-object v0, p0, $tracksManagerField
                        if-eqz v0, :null
                        invoke-virtual { v0 }, ${tracksFingerprint.method}
                        move-result-object v0
                        :null
                        return-object v0
                    """
                )
            }

            addTracksGetter("patch_getDirectCaptionTracks", CaptionTracksManagerDirectTracksFingerprint)
            addTracksGetter("patch_getAutoTranslateCaptionTracks", CaptionTracksManagerAutoTranslateTracksFingerprint)
        }

        // Override the default caption track.

        // Hook the call site and not the default track method itself,
        // so the subtitle manager instance is available.
        val trackType = DefaultCaptionTrackFingerprint.method.returnType

        Fingerprint(
            definingClass = DefaultCaptionTrackFingerprint.classDef.type,
            filters = listOf(
                methodCall(reference = DefaultCaptionTrackFingerprint.method),
                opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
            )
        ).match(DefaultCaptionTrackFingerprint.classDef).let {
            val subtitleManagerRegister = it.instructionMatches[0]
                .getInstruction<FiveRegisterInstruction>().registerC
            val moveResultIndex = it.instructionMatches[1].index
            val trackRegister = it.instructionMatches[1].getInstruction<OneRegisterInstruction>().registerA

            it.method.addInstructions(
                moveResultIndex + 1,
                """
                    invoke-static { v$subtitleManagerRegister, v$trackRegister }, $EXTENSION_CLASS->getPreferredCaptionTrack($EXTENSION_SUBTITLE_MANAGER_INTERFACE$EXTENSION_CAPTION_TRACK_INTERFACE)$EXTENSION_CAPTION_TRACK_INTERFACE
                    move-result-object v$trackRegister
                    check-cast v$trackRegister, $trackType
                """
            )
        }

        SetSubtitleTrackFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0, p1, p2 }, $EXTENSION_CLASS->onSetSubtitleTrack($EXTENSION_SUBTITLE_MANAGER_INTERFACE${EXTENSION_CAPTION_TRACK_INTERFACE}Ljava/lang/Enum;)$EXTENSION_CAPTION_TRACK_INTERFACE
                move-result-object p1
                check-cast p1, $trackType
            """
        )

        onCreateHook(EXTENSION_CLASS, "newVideoStarted")

        StartVideoInformerFingerprint.method.addInstruction(
            0,
            "invoke-static { }, $EXTENSION_CLASS->videoInformationLoaded()V"
        )
    }
}
