package app.revanced.patches.youtube.utils.fix.streamingdata

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.rawResourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.extension.Constants.SPOOF_PATH
import app.revanced.patches.shared.spoof.blockrequest.baseBlockRequestPatch
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
import app.revanced.util.cloneMutable
import app.revanced.util.copyResources
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getNode
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.inputStreamFromBundledResourceOrThrow
import app.revanced.util.returnEarly
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
import com.android.tools.smali.dexlib2.util.MethodUtil
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

private const val EXTENSION_YOUTUBE_SPOOF_PATH =
    app.revanced.patches.youtube.utils.extension.Constants.SPOOF_PATH
private const val EXTENSION_AUTO_TRACK_CLASS_DESCRIPTOR =
    "$EXTENSION_YOUTUBE_SPOOF_PATH/AudioTrackPatch;"
private const val EXTENSION_AUTO_TRACK_BUTTON_CLASS_DESCRIPTOR =
    "$EXTENSION_YOUTUBE_SPOOF_PATH/ui/AudioTrackButton;"
private const val EXTENSION_RELOAD_VIDEO_CLASS_DESCRIPTOR =
    "$EXTENSION_YOUTUBE_SPOOF_PATH/ReloadVideoPatch;"
private const val EXTENSION_RELOAD_VIDEO_BUTTON_CLASS_DESCRIPTOR =
    "$EXTENSION_YOUTUBE_SPOOF_PATH/ui/ReloadVideoButton;"

val spoofStreamingDataPatch = bytecodePatch(
    SPOOF_STREAMING_DATA.title,
    SPOOF_STREAMING_DATA.summary
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        spoofStreamingDataRawResourcePatch,
        settingsPatch,
        baseSpoofUserAgentPatch(YOUTUBE_PACKAGE_NAME),
        baseBlockRequestPatch(EXTENSION_CLASS_DESCRIPTOR),
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

    val useMobileWebClient by booleanOption(
        key = "useMobileWebClient",
        default = false,
        title = "Use Mobile Web client",
        description = "Add Mobile Web to available clients.",
        required = true
    )

    execute {

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
                            invoke-static { v$freeRegister, v$setStreamingDataRegister }, $EXTENSION_AUTO_TRACK_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;${STREAMING_DATA_OUTER_CLASS})V
                            invoke-direct { p0, v$setStreamingDataRegister, v$freeRegister }, $resultMethodType->$setStreamDataMethodName(${STREAMING_DATA_OUTER_CLASS}Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                            move-result-object v$setStreamingDataRegister
                            """
                    )

                    result.classDef.methods.filter { method ->
                        MethodUtil.isConstructor(method)
                    }.forEach { method ->
                        method.addInstruction(
                            1,
                            "invoke-static { p0 }, $EXTENSION_STREAMING_DATA_OUTER_CLASS_DESCRIPTOR->initialize($EXTENSION_STREAMING_DATA_INTERFACE)V"
                        )
                    }

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
                            MutableMethodImplementation(5),
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
                            MutableMethodImplementation(5),
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

        videoStreamingDataConstructorFingerprint.matchOrThrow(
            videoStreamingDataToStringFingerprint
        ).let {
            it.method.apply {
                val setAdaptiveFormatsMethodName = "patch_setAdaptiveFormats"

                val adaptiveFormatsFieldIndex =
                    indexOfGetAdaptiveFormatsFieldInstruction(this)
                val adaptiveFormatsRegister =
                    getInstruction<TwoRegisterInstruction>(adaptiveFormatsFieldIndex).registerA
                val videoIdIndex =
                    indexOfFirstInstructionReversedOrThrow(adaptiveFormatsFieldIndex) {
                        val reference = getReference<FieldReference>()
                        opcode == Opcode.IGET_OBJECT &&
                                reference?.type == "Ljava/lang/String;" &&
                                reference.definingClass == definingClass
                    }
                var definingClassRegister =
                    getInstruction<TwoRegisterInstruction>(videoIdIndex).registerB
                val videoIdReference =
                    getInstruction<ReferenceInstruction>(videoIdIndex).reference as FieldReference

                it.classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
                        setAdaptiveFormatsMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                "Ljava/util/List;",
                                annotations,
                                "adaptiveFormats"
                            )
                        ),
                        "Ljava/util/List;",
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(4),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                # Get video id.
                                iget-object v0, p0, $videoIdReference
                                
                                # Override adaptive formats.
                                invoke-static { v0, p1 }, $EXTENSION_CLASS_DESCRIPTOR->prioritizeVideoQuality(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
                                move-result-object p1

                                return-object p1
                                """,
                        )
                    },
                )

                addInstructions(
                    adaptiveFormatsFieldIndex + 1, """
                        # Override adaptive formats.
                        invoke-direct { v$definingClassRegister,  v$adaptiveFormatsRegister }, $definingClass->$setAdaptiveFormatsMethodName(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$adaptiveFormatsRegister
                        """
                )

                val setStreamDataMethodName = "patch_setStreamingData"
                val streamingDataIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IPUT_OBJECT &&
                            reference?.type == STREAMING_DATA_OUTER_CLASS &&
                            reference.definingClass == definingClass
                }
                val streamingDataField =
                    getInstruction<ReferenceInstruction>(streamingDataIndex).reference
                val streamingDataRegister =
                    getInstruction<TwoRegisterInstruction>(streamingDataIndex).registerA

                val setVideoIdIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>() == videoIdReference
                }
                val setVideoIdRegister =
                    getInstruction<TwoRegisterInstruction>(setVideoIdIndex).registerA
                definingClassRegister =
                    getInstruction<TwoRegisterInstruction>(setVideoIdIndex).registerB

                it.classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
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
                        MutableMethodImplementation(6),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                move-object/from16 v0, p0
                                # Get streaming data.
                                invoke-static { p2 }, $EXTENSION_CLASS_DESCRIPTOR->getStreamingData(Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                                move-result-object v1
                                if-eqz v1, :ignore
                                iput-object v1, v0, $streamingDataField
                                return-object v1
                                :ignore
                                return-object p1
                                """,
                        )
                    },
                )

                addInstructions(
                    setVideoIdIndex + 1, """
                        invoke-direct { v$definingClassRegister, v$streamingDataRegister, v$setVideoIdRegister }, $definingClass->$setStreamDataMethodName(${STREAMING_DATA_OUTER_CLASS}Ljava/lang/String;)$STREAMING_DATA_OUTER_CLASS
                        move-result-object v$streamingDataRegister
                        """
                )
            }
        }

        // Some classes that exist in YouTube are not merged.
        // Instead of relocating all classes in the extension,
        // Only the methods necessary for the patch to function are copied.
        // Note: Protobuf library included with YouTube seems to have been released before 2016.
        getEmptyRegistryFingerprint.matchOrThrow().let {
            it.classDef.methods.add(
                it.method.cloneMutable(name = "getEmptyRegistry")
            )
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

        val (brotliInputStreamClassName, brotliInputStreamMethodCall) = with(
            brotliInputStreamFingerprint.methodOrThrow()
        ) {
            Pair(definingClass, methodCall())
        }

        findMethodOrThrow(EXTENSION_UTILS_CLASS_DESCRIPTOR) {
            name == "getBrotliInputStream"
        }.addInstructions(
            0, """
                new-instance v0, $brotliInputStreamClassName
                invoke-direct {v0, p0}, $brotliInputStreamMethodCall
                return-object v0
                """
        )

        arrayOf(
            ResourceGroup(
                "raw",
                "po_token.html",
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/spoof/shared", resourceGroup)
        }

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

        // endregion

        // region patch for audio track button

        hookAudioTrackId("$EXTENSION_AUTO_TRACK_CLASS_DESCRIPTOR->setAudioTrackId(Ljava/lang/String;)V")
        hookBackgroundPlayVideoInformation("$EXTENSION_AUTO_TRACK_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        injectControl(EXTENSION_AUTO_TRACK_BUTTON_CLASS_DESCRIPTOR)

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
                    "invoke-static {v$register}, $EXTENSION_RELOAD_VIDEO_CLASS_DESCRIPTOR->setProgressBarVisibility(I)V"
                )
            }

        injectControl(EXTENSION_RELOAD_VIDEO_BUTTON_CLASS_DESCRIPTOR)

        arrayOf(
            ResourceGroup(
                "drawable",
                "revanced_reload_video.xml",
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/spoof/$directory", resourceGroup)
        }

        // endregion

        if (useMobileWebClient == true) {
            patchStatusArray += "SpoofStreamingDataMobileWeb"
        }

        patchStatusArray.forEach { methodName ->
            findMethodOrThrow("$PATCHES_PATH/PatchStatus;") {
                name == methodName
            }.returnEarly(true)
        }

        addPreference(
            arrayOf(
                "SETTINGS: SPOOF_STREAMING_DATA"
            ),
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

                // WebViewUtils.
                document("AndroidManifest.xml").use { document ->
                    val feature = document.createElement("uses-feature").apply {
                        setAttribute("android:name", "android.hardware.usb.host")
                    }
                    document.getNode("manifest").appendChild(feature)
                }
            }
        }
    }
}
