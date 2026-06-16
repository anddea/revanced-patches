/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
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
import app.morphe.patches.shared.misc.fix.proto.parseByteArrayMethod
import app.morphe.util.ResourceGroup
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.copyResources
import app.morphe.util.findFreeRegister
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.insertLiteralOverride
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
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
            )
        )
    }
}

internal fun spoofVideoStreamsPatch(
    extensionClass: String,
    mainActivityOnCreateFingerprint: Fingerprint,
    fixMediaFetchHotConfig: BytecodePatchBuilder.() -> Boolean,
    fixMediaFetchHotConfigAlternative: BytecodePatchBuilder.() -> Boolean,
    fixParsePlaybackResponseFeatureFlag: BytecodePatchBuilder.() -> Boolean,
    fixMediaSessionFeatureFlag: BytecodePatchBuilder.() -> Boolean,
    fixReelItemWatchResponseFeatureFlag: BytecodePatchBuilder.() -> Boolean,
    hookAccountIdentity: BytecodePatchBuilder.() -> Boolean,
    useNewRequestBuilderFingerprint: BytecodePatchBuilder.() -> Boolean,
    block: BytecodePatchBuilder.() -> Unit,
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    description = "Adds options to spoof the client video streams to fix playback."
) {
    block()

    dependsOn(
        fixProtoLibraryPatch,
        spoofVideoStreamsResourcePatch,
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

        if (useNewRequestBuilderFingerprint()) {
            BuildPlayerRequestURIBuilderFingerprint.let {
                it.method.apply {
                    val index = it.instructionMatches.last().index
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructionsAtControlFlowLabel(
                        index,
                        $$"""
                            invoke-static { v$$register }, $$EXTENSION_CLASS->blockGetWatchRequest(Landroid/net/Uri$Builder;)Landroid/net/Uri$Builder;
                            move-result-object v$$register
                        """
                    )
                }
            }
        } else {
            BuildPlayerRequestURIFingerprint.let {
                it.method.apply {
                    val invokeToStringIndex = it.instructionMatches.first().index
                    val uriRegister = getInstruction<FiveRegisterInstruction>(invokeToStringIndex).registerC

                    addInstructions(
                        invokeToStringIndex,
                        """
                            invoke-static { v$uriRegister }, $EXTENSION_CLASS->blockGetWatchRequest(Landroid/net/Uri;)Landroid/net/Uri;
                            move-result-object v$uriRegister
                        """
                    )
                }
            }
        }

        BuildRequestFingerprint.method.apply {
            val newRequestBuilderIndex = BuildRequestFingerprint.instructionMatches.first().index
            val buildRequestMethodURLRegister = getInstruction<FiveRegisterInstruction>(newRequestBuilderIndex).registerD
            val freeRegister = findFreeRegister(newRequestBuilderIndex, buildRequestMethodURLRegister)

            addInstructions(
                newRequestBuilderIndex,
                """
                    move-object v$freeRegister, p1
                    invoke-static { v$buildRequestMethodURLRegister, v$freeRegister }, $EXTENSION_CLASS->fetchStreams(Ljava/lang/String;Ljava/util/Map;)V
                """
            )
        }

        CreateStreamingDataFingerprint.method.apply {
            val setStreamDataMethodName = "patch_setStreamingData"
            val resultMethodType = CreateStreamingDataFingerprint.classDef.type
            val videoDetailsIndex = CreateStreamingDataFingerprint.instructionMatches.last().index
            val videoDetailsRegister = getInstruction<TwoRegisterInstruction>(videoDetailsIndex).registerA
            val videoDetailsClass = getInstruction(videoDetailsIndex).getReference<FieldReference>()!!.type

            addInstruction(
                videoDetailsIndex + 1,
                "invoke-direct { p0, v$videoDetailsRegister }, " +
                        "$resultMethodType->$setStreamDataMethodName($videoDetailsClass)V",
            )

            val setStreamingDataIndex = CreateStreamingDataFingerprint.instructionMatches.first().index

            val playerProtoClass = getInstruction(setStreamingDataIndex + 1)
                .getReference<FieldReference>()!!.definingClass

            val setStreamingDataField = getInstruction(setStreamingDataIndex).getReference<FieldReference>()

            val getStreamingDataField = getInstruction(
                indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_OBJECT && getReference<FieldReference>()?.definingClass == playerProtoClass
                },
            ).getReference<FieldReference>()

            CreateStreamingDataFingerprint.classDef.methods.add(
                ImmutableMethod(
                    resultMethodType,
                    setStreamDataMethodName,
                    listOf(ImmutableMethodParameter(videoDetailsClass, null, "videoDetails")),
                    "V",
                    AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(9),
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
                            invoke-static { v4, v3 }, $parseByteArrayMethod
                            move-result-object v5
                            check-cast v5, $playerProtoClass
    
                            # Set streaming data.
                            iget-object v6, v5, $getStreamingDataField
                            if-eqz v6, :disabled
                            iput-object v6, p0, $setStreamingDataField
                            
                            :disabled
                            return-void
                        """
                    )
                }
            )
        }

        BuildRequestFingerprint.let {
            it.method.apply {
                val insertIndex = indexOfNewUrlRequestBuilderInstruction(this)
                val register = it.instructionMatches.first()
                    .getInstruction<FiveRegisterInstruction>().registerD

                addInstructions(
                    insertIndex,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS->blockGetAttRequest(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                    """
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
                    """,
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
                returnType = mediaFetchEnumClass,
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
            ReelItemWatchResponseFeatureFlagFingerprint.let {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS->useReelItemWatchResponseFeatureFlag(Z)Z"
                )
            }
        }

        executeBlock()
    }
}
