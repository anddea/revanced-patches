package app.revanced.patches.youtube.utils.fix.streamingdata

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.extension.Constants.SPOOF_PATH
import app.revanced.patches.shared.formatStreamModelConstructorFingerprint
import app.revanced.patches.shared.spoof.blockrequest.blockRequestPatch
import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.revanced.patches.youtube.utils.audiotracks.audioTracksHookPatch
import app.revanced.patches.youtube.utils.audiotracks.hookAudioTrackId
import app.revanced.patches.youtube.utils.auth.authHookPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.revanced.patches.youtube.utils.dismiss.dismissPlayerHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList.SPOOF_STREAMING_DATA
import app.revanced.patches.youtube.utils.playercontrols.addTopControl
import app.revanced.patches.youtube.utils.playercontrols.hookTopControlButton
import app.revanced.patches.youtube.utils.playercontrols.playerControlsPatch
import app.revanced.patches.youtube.utils.playservice.is_19_34_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_50_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_10_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_14_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.request.buildRequestPatch
import app.revanced.patches.youtube.utils.request.hookBuildRequest
import app.revanced.patches.youtube.utils.settings.ResourceUtils
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.video.information.hookBackgroundPlayVideoInformation
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.patches.youtube.video.playerresponse.Hook
import app.revanced.patches.youtube.video.playerresponse.addPlayerResponseMethodHook
import app.revanced.patches.youtube.video.videoid.videoIdPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/SpoofStreamingDataPatch;"

val spoofStreamingDataPatch = bytecodePatch(
    SPOOF_STREAMING_DATA.title,
    SPOOF_STREAMING_DATA.summary
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        baseSpoofUserAgentPatch(YOUTUBE_PACKAGE_NAME),
        blockRequestPatch,
        buildRequestPatch,
        versionCheckPatch,
        playerControlsPatch,
        videoIdPatch,
        videoInformationPatch,
        audioTracksHookPatch,
        authHookPatch,
        dismissPlayerHookPatch,
    )

    val outlineIcon by booleanOption(
        key = "outlineIcon",
        default = false,
        title = "Outline icons",
        description = "Apply the outline icon.",
        required = true
    )

    val useIOSClient by booleanOption(
        key = "useIOSClient",
        default = false,
        title = "Use iOS client",
        description = "Add setting to set iOS client (Deprecated) as default client.",
    )

    execute {

        var settingArray = arrayOf(
            "SETTINGS: SPOOF_STREAMING_DATA"
        )

        var patchStatusArray = arrayOf(
            "SpoofStreamingData"
        )

        // region Get replacement streams at player requests.

        hookBuildRequest("$EXTENSION_CLASS_DESCRIPTOR->fetchStreams(Ljava/lang/String;Ljava/util/Map;)V")

        // endregion

        // region Replace the streaming data.

        val approxDurationMsReference = formatStreamModelConstructorFingerprint.matchOrThrow().let {
            with(it.method) {
                getInstruction<ReferenceInstruction>(it.patternMatch!!.startIndex).reference
            }
        }

        val streamingDataFormatsReference = with(
            videoStreamingDataConstructorFingerprint.methodOrThrow(
                videoStreamingDataToStringFingerprint
            )
        ) {
            val getFormatsFieldIndex = indexOfGetFormatsFieldInstruction(this)
            val longMaxValueIndex = indexOfLongMaxValueInstruction(this, getFormatsFieldIndex)
            val longMaxValueRegister =
                getInstruction<OneRegisterInstruction>(longMaxValueIndex).registerA
            val videoIdIndex =
                indexOfFirstInstructionOrThrow(longMaxValueIndex) {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IGET_OBJECT &&
                            reference?.type == "Ljava/lang/String;" &&
                            reference.definingClass == definingClass
                }

            val definingClassRegister =
                getInstruction<TwoRegisterInstruction>(videoIdIndex).registerB
            val videoIdReference =
                getInstruction<ReferenceInstruction>(videoIdIndex).reference

            addInstructions(
                longMaxValueIndex + 1, """
                    # Get video id.
                    iget-object v$longMaxValueRegister, v$definingClassRegister, $videoIdReference
                    
                    # Override approxDurationMs.
                    invoke-static { v$longMaxValueRegister }, $EXTENSION_CLASS_DESCRIPTOR->getApproxDurationMs(Ljava/lang/String;)J
                    move-result-wide v$longMaxValueRegister
                    """
            )
            removeInstruction(longMaxValueIndex)

            getInstruction<ReferenceInstruction>(getFormatsFieldIndex).reference
        }

        createStreamingDataFingerprint.matchOrThrow(createStreamingDataParentFingerprint)
            .let { result ->
                result.method.apply {
                    val setStreamDataMethodName = "patch_setStreamingData"
                    val calcApproxDurationMsMethodName = "patch_calcApproxDurationMs"
                    val resultClassDef = result.classDef
                    val resultMethodType = resultClassDef.type
                    val setStreamingDataIndex = result.patternMatch!!.startIndex
                    val setStreamingDataField =
                        getInstruction(setStreamingDataIndex).getReference<FieldReference>()
                            .toString()

                    val playerProtoClass =
                        getInstruction(setStreamingDataIndex + 1).getReference<FieldReference>()!!.definingClass
                    val protobufClass =
                        protobufClassParseByteBufferFingerprint.definingClassOrThrow()

                    val getStreamingDataField = instructions.find { instruction ->
                        instruction.opcode == Opcode.IGET_OBJECT &&
                                instruction.getReference<FieldReference>()?.definingClass == playerProtoClass
                    }?.getReference<FieldReference>()
                        ?: throw PatchException("Could not find getStreamingDataField")

                    val videoDetailsIndex = result.patternMatch!!.endIndex
                    val videoDetailsRegister =
                        getInstruction<TwoRegisterInstruction>(videoDetailsIndex).registerA
                    val videoDetailsClass =
                        getInstruction(videoDetailsIndex).getReference<FieldReference>()!!.type

                    addInstruction(
                        videoDetailsIndex + 1,
                        "invoke-direct { p0, v$videoDetailsRegister }, " +
                                "$resultMethodType->$setStreamDataMethodName($videoDetailsClass)V",
                    )

                    result.classDef.methods.add(
                        ImmutableMethod(
                            resultMethodType,
                            setStreamDataMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    videoDetailsClass,
                                    annotations,
                                    "videoDetails"
                                )
                            ),
                            "V",
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(9),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->isSpoofingEnabled()Z
                                    move-result v0
                                    if-eqz v0, :disabled
                                    
                                    # Get video id.
                                    iget-object v2, p1, $videoDetailsClass->c:Ljava/lang/String;
                                    if-eqz v2, :disabled
                                    
                                    # Get streaming data.
                                    invoke-static { v2 }, $EXTENSION_CLASS_DESCRIPTOR->getStreamingData(Ljava/lang/String;)Ljava/nio/ByteBuffer;
                                    move-result-object v3
                                    
                                    if-eqz v3, :disabled
                                    
                                    # Parse streaming data.
                                    sget-object v4, $playerProtoClass->a:$playerProtoClass
                                    invoke-static { v4, v3 }, $protobufClass->parseFrom(${protobufClass}Ljava/nio/ByteBuffer;)$protobufClass
                                    move-result-object v5
                                    check-cast v5, $playerProtoClass
                                    
                                    iget-object v6, v5, $getStreamingDataField
                                    if-eqz v6, :disabled
                                    
                                    # Caculate approxDurationMs.
                                    invoke-direct { p0, v2 }, $resultMethodType->$calcApproxDurationMsMethodName(Ljava/lang/String;)V
                                    
                                    # Set spoofed streaming data.
                                    iput-object v6, p0, $setStreamingDataField
                                    
                                    :disabled
                                    return-void
                                    """,
                            )
                        },
                    )

                    resultClassDef.methods.add(
                        ImmutableMethod(
                            resultMethodType,
                            calcApproxDurationMsMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    "Ljava/lang/String;",
                                    annotations,
                                    "videoId"
                                )
                            ),
                            "V",
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(12),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                    # Get video format list.
                                    iget-object v0, p0, $setStreamingDataField
                                    iget-object v0, v0, $streamingDataFormatsReference
                                    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                                    move-result-object v0
                                    
                                    # Initialize approxDurationMs field.
                                    const-wide v1, 0x7fffffffffffffffL
                                    
                                    :loop
                                    # Loop over all video formats to get the approxDurationMs
                                    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
                                    move-result v3
                                    const-wide/16 v4, 0x0
                                    
                                    if-eqz v3, :exit
                                    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                                    move-result-object v3
                                    check-cast v3, ${(approxDurationMsReference as FieldReference).definingClass}
                                    
                                    # Get approxDurationMs from format
                                    iget-wide v6, v3, $approxDurationMsReference
                                    
                                    # Compare with zero to make sure approxDurationMs is not negative
                                    cmp-long v8, v6, v4
                                    if-lez v8, :loop
                                    
                                    # Only use the min value of approxDurationMs
                                    invoke-static {v1, v2, v6, v7}, Ljava/lang/Math;->min(JJ)J
                                    move-result-wide v1
                                    goto :loop
                                    
                                    :exit
                                    # Save approxDurationMs to integrations
                                    invoke-static { p1, v1, v2 }, $EXTENSION_CLASS_DESCRIPTOR->setApproxDurationMs(Ljava/lang/String;J)V
                                    
                                    return-void
                                    """,
                            )
                        },
                    )
                }
            }

        // endregion

        // region Remove /videoplayback request body to fix playback.
        // This is needed when using iOS client as streaming data source.

        buildMediaDataSourceFingerprint.methodOrThrow().apply {
            val targetIndex = instructions.lastIndex

            addInstructions(
                targetIndex,
                """
                    # Field a: Stream uri.
                    # Field c: Http method.
                    # Field d: Post data.
                    move-object/from16 v0, p0
                    iget-object v1, v0, $definingClass->a:Landroid/net/Uri;
                    iget v2, v0, $definingClass->c:I
                    iget-object v3, v0, $definingClass->d:[B
                    invoke-static { v1, v2, v3 }, $EXTENSION_CLASS_DESCRIPTOR->removeVideoPlaybackPostBody(Landroid/net/Uri;I[B)[B
                    move-result-object v1
                    iput-object v1, v0, $definingClass->d:[B
                    """,
            )
        }

        // endregion

        // region Append spoof info.

        nerdsStatsVideoFormatBuilderFingerprint.methodOrThrow().apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->appendSpoofedClient(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """
                )
            }
        }

        // endregion

        // region Fix iOS livestream current time.

        hlsCurrentTimeFingerprint.injectLiteralInstructionBooleanCall(
            HLS_CURRENT_TIME_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->fixHLSCurrentTime(Z)Z"
        )

        // endregion

        // region Skip response encryption in OnesiePlayerRequest

        if (is_19_34_or_greater) {
            onesieEncryptionFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                ONESIE_ENCRYPTION_FEATURE_FLAG,
                "$EXTENSION_CLASS_DESCRIPTOR->skipResponseEncryption(Z)Z"
            )

            if (is_19_50_or_greater) {
                playbackStartDescriptorFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                    PLAYBACK_START_CHECK_ENDPOINT_USED_FEATURE_FLAG,
                    "$EXTENSION_CLASS_DESCRIPTOR->usePlaybackStartFeatureFlag(Z)Z"
                )

                // In 20.14 the flag was merged with 20.03 start playback flag.
                if (is_20_10_or_greater && !is_20_14_or_greater) {
                    onesieEncryptionAlternativeFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                        ONESIE_ENCRYPTION_ALTERNATIVE_FEATURE_FLAG,
                        "$EXTENSION_CLASS_DESCRIPTOR->skipResponseEncryption(Z)Z"
                    )
                }
            }

            settingArray += "SETTINGS: SKIP_RESPONSE_ENCRYPTION"
        }

        if (useIOSClient == true) {
            settingArray += "SETTINGS: USE_IOS_DEPRECATED"
            patchStatusArray += "SpoofStreamingDataIOS"
        }

        // endregion

        // region patch for audio track button

        val spoofPath = app.revanced.patches.youtube.utils.extension.Constants.SPOOF_PATH
        addPlayerResponseMethodHook(
            Hook.PlayerParameterBeforeVideoId(
                "$spoofPath/AudioTrackPatch;->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;"
            )
        )
        hookAudioTrackId("$spoofPath/AudioTrackPatch;->setAudioTrackId(Ljava/lang/String;)V")
        hookBackgroundPlayVideoInformation("$spoofPath/AudioTrackPatch;->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        hookTopControlButton("$spoofPath/ui/AudioTrackButtonController;")

        val directory = if (outlineIcon == true)
            "outline"
        else
            "default"

        arrayOf(
            ResourceGroup(
                "drawable",
                "revanced_audio_track.xml",
            )
        ).forEach { resourceGroup ->
            ResourceUtils.getContext().copyResources("youtube/spoof/$directory", resourceGroup)
        }

        // endregion

        patchStatusArray.forEach { methodName ->
            findMethodOrThrow("$PATCHES_PATH/PatchStatus;") {
                name == methodName
            }.replaceInstruction(
                0,
                "const/4 v0, 0x1"
            )
        }

        addPreference(
            settingArray,
            SPOOF_STREAMING_DATA
        )
    }

    // Add the audio track button last in order to place it to the left of the subtitle button.
    finalize {
        addTopControl(
            "youtube/spoof/shared",
            "@+id/revanced_audio_track_button",
            "@+id/revanced_audio_track_button"
        )
    }
}
