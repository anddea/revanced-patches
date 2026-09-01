/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.shared.misc.spoof

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter.Companion.opcodesToFilters
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.misc.fix.proto.parseByteArrayMethodRef
import app.morphe.patches.shared.misc.request.buildRequestPatch
import app.morphe.patches.shared.misc.request.hookBuildRequest
import app.morphe.patches.youtube.utils.patch.PatchList.SPOOF_VIDEO_STREAMS
import app.morphe.util.ResourceGroup
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.copyResources
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.insertLiteralOverride
import app.morphe.util.registersUsed
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

internal const val EXTENSION_CLASS =
    "Lapp/morphe/extension/shared/spoof/SpoofVideoStreamsPatch;"

private val spoofVideoStreamsResourcePatch = resourcePatch {
    execute {
        copyResources(
            "spoof",
            ResourceGroup(
                "raw",
                "astring-1.9.0.min.js",
                "meriyah-6.1.4.min.js",
                "polyfill.js",
                "yt.solver.core.js",
                "yt.solver.wrapper.js",
                "po_token.html",
            )
        )
    }
}

internal fun spoofVideoStreamsPatch(
    extensionClass: String,
    mainActivityOnCreateFingerprint: Fingerprint,
    fixMediaFetchHotConfig: BytecodePatchBuilder.() -> Boolean = { false },
    fixMediaFetchHotConfigAlternative: BytecodePatchBuilder.() -> Boolean,
    fixParsePlaybackResponseFeatureFlag: BytecodePatchBuilder.() -> Boolean,
    fixMediaSessionFeatureFlag: BytecodePatchBuilder.() -> Boolean,
    fixReelItemWatchResponseFeatureFlag: BytecodePatchBuilder.() -> Boolean,
    hookAccountIdentity: BytecodePatchBuilder.() -> Boolean = { false },
    restoreMissingCuepointMethod: BytecodePatchBuilder.() -> Boolean = { false },
    block: BytecodePatchBuilder.() -> Unit,
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    name = SPOOF_VIDEO_STREAMS.title,
    description = SPOOF_VIDEO_STREAMS.summary,
) {
    block()

    dependsOn(
        fixProtoLibraryPatch,
        spoofVideoStreamsResourcePatch,
        buildRequestPatch
    )

    execute {
        mainActivityOnCreateFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->setMainActivity(Landroid/app/Activity;)V       
                invoke-static { }, $extensionClass->setClientOrderToUse()V   
            """
        )

        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        BuildInitPlaybackRequestFingerprint.let {
            it.method.apply {
                val moveUriStringIndex = it.instructionMatches.first().index
                val targetRegister = getInstruction<OneRegisterInstruction>(moveUriStringIndex).registerA

                addInstructions(
                    moveUriStringIndex + 1,
                    """
                        invoke-static { v$targetRegister }, $EXTENSION_CLASS->blockInitPlaybackRequest(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                    """
                )
            }
        }

        BuildInnerTubeProtoRequestUriFingerprint.let {
            it.method.apply {
                val match = it.instructionMatches.last()
                val index = match.index
                val register = match.instruction.registersUsed[0]

                addInstructionsAtControlFlowLabel(
                    index,
                    $$"""
                        invoke-static { v$$register }, $$EXTENSION_CLASS->blockGetWatchRequest(Landroid/net/Uri$Builder;)Landroid/net/Uri$Builder;
                        move-result-object v$$register
                    """
                )
            }
        }

        hookBuildRequest(
            "$EXTENSION_CLASS->fetchStreams(Ljava/lang/String;Ljava/util/Map;)V"
        )

        CreateStreamingDataFingerprint.let {
            it.method.apply {
                val videoDetailsMatch = it.instructionMatches[5]
                val videoDetailsIndex = videoDetailsMatch.index
                val videoDetailsRegister = getInstruction<TwoRegisterInstruction>(videoDetailsIndex).registerA
                val videoDetailsClass = videoDetailsMatch.instruction.getReference<FieldReference>()!!.type

                val playerProtoClass = parameterTypes.first().toString()
                val getStreamingDataField = it.instructionMatches.first().instruction.getReference<FieldReference>()!!
                val setStreamingDataField = it.instructionMatches[1].instruction.getReference<FieldReference>()!!
                val setPlayerConfigField = it.instructionMatches.last().instruction.getReference<FieldReference>()!!
                val playerConfigClass = setPlayerConfigField.type
                val (mediaCommonConfigField, mediaUstreamerRequestConfig) =
                    with(abrStateDataFingerprint(playerConfigClass)) {
                        Pair(
                            instructionMatches[1].instruction.getReference<FieldReference>()!!,
                            instructionMatches[2].instruction.getReference<FieldReference>()!!,
                        )
                    }

                val (createBuilderMethod, mergeFromMethod) =
                    with(PlayerConfigBuilderFingerprint) {
                        Pair(
                            instructionMatches[2].instruction.getReference<MethodReference>()!!,
                            instructionMatches[4].instruction.getReference<MethodReference>()!!,
                        )
                    }

                val (castReference, buildMethod) =
                    with(PlayerConfigBuilderFingerprint) {
                        Pair(
                            instructionMatches[5].instruction.getReference<TypeReference>()!!,
                            instructionMatches[6].instruction.getReference<MethodReference>()!!
                        )
                    }

                val castInstruction = if (castReference.type != buildMethod.definingClass) """
                    check-cast v4, $castReference
                """ else """
                    nop
                """

                val helperMethod = ImmutableMethod(
                    it.classDef.type,
                    "patch_setStreamingData",
                    listOf(
                        ImmutableMethodParameter(videoDetailsClass, null, null),
                        ImmutableMethodParameter(playerProtoClass, null, null)
                    ),
                    "V",
                    AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(11),
                ).toMutable().apply {
                    addInstructionsWithLabels(
                        0,
                        """
                            invoke-static { }, $EXTENSION_CLASS->isSpoofingEnabled()Z
                            move-result v0
                            if-eqz v0, :disabled

                            # Get video ID.
                            iget-object v2, p1, $videoDetailsClass->c:Ljava/lang/String;
                            if-eqz v2, :disabled

                            # Get streaming data.
                            invoke-static { v2 }, $EXTENSION_CLASS->getStreamingData(Ljava/lang/String;)[B
                            move-result-object v3
                            if-eqz v3, :disabled

                            # Parse streaming data.
                            sget-object v4, $playerProtoClass->a:$playerProtoClass
                            invoke-static { v4, v3 }, ${parseByteArrayMethodRef.get()!!}
                            move-result-object v5
                            check-cast v5, $playerProtoClass

                            # Set streaming data.
                            iget-object v6, v5, $getStreamingDataField
                            if-eqz v6, :disabled
                            iput-object v6, p0, $setStreamingDataField

                            # Get player config.
                            invoke-static { v2 }, $EXTENSION_CLASS->getPlayerConfig(Ljava/lang/String;)[B
                            move-result-object v3
                            if-eqz v3, :disabled
                            sget-object v4, $playerConfigClass->a:$playerConfigClass
                            invoke-virtual { v4 }, $createBuilderMethod
                            move-result-object v4
                            $castInstruction
                            invoke-static { }, Lcom/google/protobuf/ExtensionRegistryLite;->getGeneratedRegistry()Lcom/google/protobuf/ExtensionRegistryLite;
                            move-result-object v5
                            invoke-virtual { v4, v3, v5 }, $mergeFromMethod
                            move-result-object v5
                            check-cast v5, $castReference
                            invoke-virtual { v5 }, $buildMethod
                            move-result-object v5
                            check-cast v5, $playerConfigClass

                            # Check if player config contains android media lib config.
                            invoke-static { v2 }, $EXTENSION_CLASS->hasAndroidMedia(Ljava/lang/String;)Z
                            move-result v3
                            if-nez v3, :override_all_player_config

                            iget-object v6, v5, $mediaCommonConfigField
                            if-eqz v6, :disabled
                            iget-object v7, v6, $mediaUstreamerRequestConfig
                            if-eqz v7, :disabled

                            # Set media uStreamer request config.
                            iget-object v5, p2, $setPlayerConfigField
                            iget-object v6, v5, $mediaCommonConfigField
                            iput-object v7, v6, $mediaUstreamerRequestConfig
                            iput-object v6, v5, $mediaCommonConfigField

                            :override_all_player_config
                            # Set player config.
                            iput-object v5, p2, $setPlayerConfigField

                            :disabled
                            return-void
                        """
                    )
                }

                it.classDef.methods.add(helperMethod)

                addInstruction(
                    videoDetailsIndex + 1,
                    "invoke-direct { p0, v$videoDetailsRegister, p1 }, $helperMethod"
                )
            }
        }

        BuildMediaDataSourceFingerprint.method.apply {
            val targetIndex =
                indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_VOID)

            addInstructions(
                targetIndex,
                """
                    # Field a: Stream uri.
                    # Field c: Http method.
                    # Field d: Post data.
                    move-object v0, p0
                    iget-object v1, v0, $definingClass->a:Landroid/net/Uri;
                    iget v2, v0, $definingClass->c:I
                    iget-object v3, v0, $definingClass->d:[B
                    invoke-static { v1, v2, v3 }, $EXTENSION_CLASS->removeVideoPlaybackPostBody(Landroid/net/Uri;I[B)[B
                    move-result-object v1
                    iput-object v1, v0, $definingClass->d:[B
                """
            )
        }

        NerdsStatsVideoFormatBuilderFingerprint.method.apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS->appendSpoofedClient(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                    """
                )
            }
        }

        HlsCurrentTimeFingerprint.let {
            it.method.insertLiteralOverride(
                it.instructionMatches.first().index,
                "$EXTENSION_CLASS->fixHLSCurrentTime(Z)Z"
            )
        }

        with(MediaFetchEnumConstructorFingerprint.method) {
            val mediaFetchEnumClass = definingClass
            val stringIndex = MediaFetchEnumConstructorFingerprint.stringMatches.last().index
            val sabrFieldIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                opcode == Opcode.SPUT_OBJECT &&
                        getReference<FieldReference>()?.type == mediaFetchEnumClass
            }
            val sabrFieldReference = getInstruction<ReferenceInstruction>(sabrFieldIndex).reference as FieldReference

            Fingerprint(
                returnType = definingClass,
                filters = opcodesToFilters(
                    Opcode.SGET_OBJECT,
                    Opcode.RETURN_OBJECT,
                ),
                custom = { method, _ ->
                    !method.parameterTypes.isEmpty()
                }
            ).method.addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $EXTENSION_CLASS->disableSABR()Z
                    move-result v0
                    if-eqz v0, :ignore
                    sget-object v0, $sabrFieldReference
                    return-object v0
                    :ignore
                    nop
                """
            )
        }

        if (hookAccountIdentity()) {
            accountIdentityFingerprint.method.addInstruction(
                0,
                "invoke-static { p3, p4 }, $EXTENSION_CLASS->setAccountIdentity(Ljava/lang/String;Z)V"
            )
        }

        if (fixMediaFetchHotConfig()) {
            MediaFetchHotConfigFingerprint.let {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS->useMediaFetchHotConfigReplacement(Z)Z"
                )
            }
        }

        if (fixMediaFetchHotConfigAlternative()) {
            MediaFetchHotConfigAlternativeFingerprint.let {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS->useMediaFetchHotConfigReplacement(Z)Z"
                )
            }
        }

        if (fixParsePlaybackResponseFeatureFlag()) {
            PlaybackStartDescriptorFeatureFlagFingerprint.let {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS->usePlaybackStartFeatureFlag(Z)Z"
                )
            }
        }

        if (fixMediaSessionFeatureFlag()) {
            MediaSessionFeatureFlagFingerprint.let {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS->useMediaSessionFeatureFlag(Z)Z"
                )
            }
        }

        if (fixReelItemWatchResponseFeatureFlag()) {
            ReelItemWatchResponseFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS->useReelItemWatchResponseFeatureFlag(Z)Z"
                )
            }
        }

        // Restore missing method sometimes called by
        // com.google.android.libraries.youtube.media.interfaces.NetFetchCallbacks$CppProxy
        // Method is present in YT 21.13+ but not older targets.
        if (restoreMissingCuepointMethod()) {
            CuepointListFingerprint.classDef.apply {
                if (methods.none {
                        it.name == "parseFrom"
                                && it.parameterTypes.isNotEmpty()
                                && it.parameterTypes.first() == "Ljava/nio/ByteBuffer;"
                    }
                ) {
                    val cuepointListType = $$"Lcom/google/android/apps/youtube/proto/streaming/CuepointListOuterClass$CuepointList;"
                    val cueField = fields.single {
                        it.type == cuepointListType
                    }
                    val superClass = superclass!!

                    // Verify the superclass method exists.
                    Fingerprint(
                        definingClass = superClass,
                        name = "parseFrom",
                        returnType = superClass,
                        parameters = listOf(
                            superClass,
                            "Ljava/nio/ByteBuffer;",
                            "Lcom/google/protobuf/ExtensionRegistryLite;"
                        )
                    ).method

                    methods.add(
                        ImmutableMethod(
                            type,
                            "parseFrom",
                            listOf(
                                ImmutableMethodParameter("Ljava/nio/ByteBuffer;", null, null),
                                ImmutableMethodParameter(
                                    "Lcom/google/protobuf/ExtensionRegistryLite;",
                                    null,
                                    null
                                )
                            ),
                            cuepointListType,
                            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                            null,
                            null,
                            MutableMethodImplementation(3),
                        ).toMutable().apply {
                            addInstructions(
                                0,
                                """    
                                    sget-object v0, $cueField
                                    invoke-static { v0, p0, p1 }, $superClass->parseFrom(${superClass}Ljava/nio/ByteBuffer;Lcom/google/protobuf/ExtensionRegistryLite;)$superClass
                                    move-result-object p0
                                    check-cast p0, $cuepointListType
                                    return-object p0
                                """
                            )
                        }
                    )
                }
            }
        }

        executeBlock()
    }
}
