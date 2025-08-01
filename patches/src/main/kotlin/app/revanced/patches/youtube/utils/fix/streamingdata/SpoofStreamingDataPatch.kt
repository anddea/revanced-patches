package app.revanced.patches.youtube.utils.fix.streamingdata

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.rawResourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.extension.Constants.SPOOF_PATH
import app.revanced.patches.shared.spoof.blockrequest.blockRequestPatch
import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.revanced.patches.youtube.utils.audiotracks.audioTracksHookPatch
import app.revanced.patches.youtube.utils.audiotracks.hookAudioTrackId
import app.revanced.patches.youtube.utils.auth.authHookPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.revanced.patches.youtube.utils.dismiss.dismissPlayerHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList
import app.revanced.patches.youtube.utils.patch.PatchList.SPOOF_STREAMING_DATA
import app.revanced.patches.youtube.utils.playercontrols.addTopControl
import app.revanced.patches.youtube.utils.playercontrols.injectControl
import app.revanced.patches.youtube.utils.playercontrols.playerControlsPatch
import app.revanced.patches.youtube.utils.playservice.is_19_34_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_50_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_10_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_14_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.request.buildRequestPatch
import app.revanced.patches.youtube.utils.request.hookBuildRequest
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.video.information.hookBackgroundPlayVideoInformation
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.patches.youtube.video.playerresponse.Hook
import app.revanced.patches.youtube.video.playerresponse.addPlayerResponseMethodHook
import app.revanced.patches.youtube.video.videoid.videoIdPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.copyResources
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.inputStreamFromBundledResourceOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.notExists

private lateinit var context: ResourcePatchContext

private val spoofStreamingDataRawResourcePatch = rawResourcePatch(
    description = "spoofStreamingDataRawResourcePatch"
) {
    execute {
        context = this
    }
}

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/SpoofStreamingDataPatch;"
private const val EXTENSION_STREAMING_DATA_OUTER_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/StreamingDataOuterClassPatch;"
private const val EXTENSION_STREAMING_DATA_INTERFACE =
    "$SPOOF_PATH/StreamingDataOuterClassPatch${'$'}StreamingDataMessage;"

val spoofStreamingDataPatch = bytecodePatch(
    SPOOF_STREAMING_DATA.title,
    SPOOF_STREAMING_DATA.summary
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        spoofStreamingDataRawResourcePatch,
        settingsPatch,
        baseSpoofUserAgentPatch(YOUTUBE_PACKAGE_NAME),
        blockRequestPatch,
        buildRequestPatch,
        sharedResourceIdPatch,
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

        createStreamingDataFingerprint.matchOrThrow(createStreamingDataParentFingerprint)
            .let { result ->
                result.method.apply {
                    val parseResponseProtoMethodName = "parseFrom"

                    val setStreamDataMethodName = "patch_setStreamingData"
                    val resultClassDef = result.classDef
                    val resultMethodType = resultClassDef.type
                    val setStreamingDataIndex = result.patternMatch!!.startIndex
                    val setStreamingDataRegister =
                        getInstruction<TwoRegisterInstruction>(setStreamingDataIndex).registerA

                    val playerProtoClass =
                        getInstruction(setStreamingDataIndex + 1).getReference<FieldReference>()!!.definingClass
                    val protobufClass =
                        protobufClassParseByteBufferFingerprint.definingClassOrThrow()

                    val getStreamingDataField = instructions.find { instruction ->
                        instruction.opcode == Opcode.IGET_OBJECT &&
                                instruction.getReference<FieldReference>()?.definingClass == playerProtoClass
                    }?.getReference<FieldReference>()
                        ?: throw PatchException("Could not find getStreamingDataField")

                    val getVideoDetailsIndex = setStreamingDataIndex + 1
                    val getVideoDetailsFallbackIndex = setStreamingDataIndex + 3
                    val getVideoDetailsField =
                        getInstruction(getVideoDetailsIndex).getReference<FieldReference>()!!
                    val getVideoDetailsFallbackField =
                        getInstruction(getVideoDetailsFallbackIndex).getReference<FieldReference>()!!
                    val freeRegister = implementation!!.registerCount - parameters.size - 2

                    addInstructionsAtControlFlowLabel(
                        setStreamingDataIndex, """
                            iget-object v$freeRegister, p1, $getVideoDetailsField
                            if-nez v$freeRegister, :ignore
                            sget-object v$freeRegister, $getVideoDetailsFallbackField
                            :ignore
                            iget-object v$freeRegister, v$freeRegister, ${getVideoDetailsField.type}->c:Ljava/lang/String;
                            invoke-direct { p0, v$setStreamingDataRegister, v$freeRegister }, $resultMethodType->$setStreamDataMethodName(${STREAMING_DATA_OUTER_CLASS}Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                            move-result-object v$setStreamingDataRegister
                            """
                    )

                    addInstruction(
                        1,
                        "invoke-static { p0 }, $EXTENSION_STREAMING_DATA_OUTER_CLASS_DESCRIPTOR->initialize($EXTENSION_STREAMING_DATA_INTERFACE)V"
                    )

                    resultClassDef.interfaces.add(EXTENSION_STREAMING_DATA_INTERFACE)

                    result.classDef.methods.add(
                        ImmutableMethod(
                            resultMethodType,
                            parseResponseProtoMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    "Ljava/nio/ByteBuffer;",
                                    annotations,
                                    "responseProto"
                                )
                            ),
                            STREAMING_DATA_OUTER_CLASS,
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(4),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                    # Check buffer is not null.
                                    if-nez p1, :parse
                                    const/4 v0, 0x0
                                    return-object v0
                                    :parse

                                    # Parse streaming data.
                                    sget-object v0, $playerProtoClass->a:$playerProtoClass
                                    invoke-static { v0, p1 }, $protobufClass->parseFrom(${protobufClass}Ljava/nio/ByteBuffer;)$protobufClass
                                    move-result-object v0
                                    check-cast v0, $playerProtoClass
                                    iget-object v0, v0, $getStreamingDataField
                                    return-object v0
                                    """,
                            )
                        },
                    )

                    result.classDef.methods.add(
                        ImmutableMethod(
                            resultMethodType,
                            setStreamDataMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    STREAMING_DATA_OUTER_CLASS,
                                    annotations,
                                    "streamingDataOuterClass"
                                ),
                                ImmutableMethodParameter(
                                    "Ljava/lang/String;",
                                    annotations,
                                    "videoId"
                                )
                            ),
                            STREAMING_DATA_OUTER_CLASS,
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(4),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                    # Get streaming data.
                                    invoke-static { p2 }, $EXTENSION_CLASS_DESCRIPTOR->getStreamingData(Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                                    move-result-object v0
                                    if-eqz v0, :ignore                                    
                                    return-object v0
                                    :ignore
                                    return-object p1
                                    """,
                            )
                        },
                    )
                }
            }

        addPlayerResponseMethodHook(
            Hook.PlayerParameterBeforeVideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;"
            )
        )

        findMethodOrThrow(EXTENSION_UTILS_CLASS_DESCRIPTOR) {
            name == "setContext"
        }.apply {
            addInstruction(
                implementation!!.instructions.lastIndex,
                "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->initializeJavascript()V"
            )
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

        nerdsStatsFormatBuilderFingerprint.methodOrThrow().apply {
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
        }

        if (useIOSClient == true) {
            settingArray += "SETTINGS: USE_IOS_DEPRECATED"
            patchStatusArray += "SpoofStreamingDataIOS"
        }

        // endregion

        // region patch for audio track button

        val spoofPath = app.revanced.patches.youtube.utils.extension.Constants.SPOOF_PATH
        hookAudioTrackId("$spoofPath/AudioTrackPatch;->setAudioTrackId(Ljava/lang/String;)V")
        hookBackgroundPlayVideoInformation("$spoofPath/AudioTrackPatch;->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        injectControl("$spoofPath/ui/AudioTrackButton;")

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
            context.copyResources("youtube/spoof/$directory", resourceGroup)
        }

        // endregion

        // region patch for reload video button

        progressBarVisibilityFingerprint
            .methodOrThrow(progressBarVisibilityParentFingerprint).apply {
                val index = indexOfProgressBarVisibilityInstruction(this)
                val register = getInstruction<FiveRegisterInstruction>(index).registerD

                addInstructionsAtControlFlowLabel(
                    index,
                    "invoke-static {v$register}, $spoofPath/ReloadVideoPatch;->setProgressBarVisibility(I)V"
                )
            }

        injectControl("$spoofPath/ui/ReloadVideoButton;")

        arrayOf(
            ResourceGroup(
                "drawable",
                "revanced_reload_video.xml",
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/spoof/$directory", resourceGroup)
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
            "@+id/revanced_reload_video_button"
        )

        // When the app is installed via mounting (excluding the GmsCore support patch), the j2v8 library is not loaded.
        // Due to the large size of the j2v8 library, it is only copied when the GmsCore support patch is included.
        // This is not a major issue, as root users do not need the Spoof streaming data patch.
        if (PatchList.GMSCORE_SUPPORT.included == true) {
            // Copy the j2v8 library.
            context.apply {
                setOf(
                    "arm64-v8a",
                    "armeabi-v7a",
                    "x86",
                    "x86_64"
                ).forEach { lib ->
                    val libraryDirectory = get("lib")
                    val architectureDirectory = libraryDirectory.resolve(lib)

                    if (architectureDirectory.exists()) {
                        val libraryFile = architectureDirectory.resolve("libj2v8.so")

                        val libraryDirectoryPath = libraryDirectory.toPath()
                        if (libraryDirectoryPath.notExists()) {
                            Files.createDirectories(libraryDirectoryPath)
                        }
                        val architectureDirectoryPath = architectureDirectory.toPath()
                        if (architectureDirectoryPath.notExists()) {
                            Files.createDirectories(architectureDirectoryPath)
                        }
                        val libraryPath = libraryFile.toPath()
                        Files.createFile(libraryPath)

                        val inputStream = inputStreamFromBundledResourceOrThrow(
                            "youtube/spoof/jniLibs",
                            "$lib/libj2v8.so"
                        )

                        Files.copy(
                            inputStream,
                            libraryPath,
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    }
                }
            }
        }
    }
}
