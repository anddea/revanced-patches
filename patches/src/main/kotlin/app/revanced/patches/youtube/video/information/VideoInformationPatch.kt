package app.revanced.patches.youtube.video.information

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.shared.FIXED_RESOLUTION_STRING
import app.revanced.patches.shared.formatStreamModelToStringFingerprint
import app.revanced.patches.shared.mdxPlayerDirectorSetVideoStageFingerprint
import app.revanced.patches.shared.playbackStartParametersConstructorFingerprint
import app.revanced.patches.shared.playbackStartParametersToStringFingerprint
import app.revanced.patches.shared.videoLengthFingerprint
import app.revanced.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE
import app.revanced.patches.youtube.utils.YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
import app.revanced.patches.youtube.utils.extension.Constants.SHARED_PATH
import app.revanced.patches.youtube.utils.extension.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.videoEndFingerprint
import app.revanced.patches.youtube.utils.videoIdFingerprintShorts
import app.revanced.patches.youtube.video.playerresponse.Hook
import app.revanced.patches.youtube.video.playerresponse.addPlayerResponseMethodHook
import app.revanced.patches.youtube.video.playerresponse.playerResponseMethodHookPatch
import app.revanced.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.revanced.patches.youtube.video.videoid.hookVideoId
import app.revanced.patches.youtube.video.videoid.videoIdPatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.cloneMutable
import app.revanced.util.findFieldFromToString
import app.revanced.util.findMethodFromToString
import app.revanced.util.findMethodOrThrow
import app.revanced.util.findMutableClassOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.fingerprint.originalMethodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/VideoInformation;"

private const val EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VideoQualityPatch;"

private const val EXTENSION_VIDEO_QUALITY_MENU_INTERFACE =
    "$VIDEO_PATH/VideoQualityPatch\$VideoQualityMenuInterface;"

private const val REGISTER_PLAYER_RESPONSE_MODEL = 8

private const val REGISTER_CHANNEL_ID = 0
private const val REGISTER_CHANNEL_NAME = 1
private const val REGISTER_VIDEO_ID = 2
private const val REGISTER_VIDEO_TITLE = 3
private const val REGISTER_VIDEO_LENGTH = 4

@Suppress("unused")
private const val REGISTER_VIDEO_LENGTH_DUMMY = 5
private const val REGISTER_VIDEO_IS_LIVE = 6

private lateinit var channelIdMethodCall: String
private lateinit var channelNameMethodCall: String
private lateinit var videoIdMethodCall: String
private lateinit var videoTitleMethodCall: String
private lateinit var videoLengthMethodCall: String
private lateinit var videoIsLiveMethodCall: String

private lateinit var videoInformationMethod: MutableMethod
private lateinit var backgroundVideoInformationMethod: MutableMethod
private lateinit var shortsVideoInformationMethod: MutableMethod

/**
 * Used in [videoEndFingerprint] and [mdxPlayerDirectorSetVideoStageFingerprint].
 * Since both classes are inherited from the same class,
 * [videoEndFingerprint] and [mdxPlayerDirectorSetVideoStageFingerprint] always have the same [seekSourceEnumType] and [seekSourceMethodName].
 */
private var seekSourceEnumType = ""
private var seekSourceMethodName = ""
private var seekRelativeSourceMethodName = ""
private var cloneSeekRelativeSourceMethod = false

private lateinit var playerConstructorMethod: MutableMethod
private var playerConstructorInsertIndex = -1

private lateinit var mdxConstructorMethod: MutableMethod
private var mdxConstructorInsertIndex = -1

private lateinit var videoTimeConstructorMethod: MutableMethod
private var videoTimeConstructorInsertIndex = 2

private lateinit var setPlaybackSpeedMethodReference: MethodReference

// Used by other patches.
internal lateinit var speedSelectionInsertMethod: MutableMethod
internal lateinit var videoEndMethod: MutableMethod

val videoInformationPatch = bytecodePatch(
    description = "videoInformationPatch",
) {
    dependsOn(
        playerResponseMethodHookPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        videoIdPatch
    )

    execute {
        fun cloneSeekRelativeSourceMethod(mutableClass: MutableClass) {
            if (!cloneSeekRelativeSourceMethod) return

            val methods = mutableClass.methods

            methods.find { method ->
                method.name == seekRelativeSourceMethodName
            }?.apply {
                methods.add(
                    cloneMutable(
                        returnType = "Z"
                    ).apply {
                        val lastIndex = implementation!!.instructions.lastIndex

                        removeInstruction(lastIndex)
                        addInstructions(
                            lastIndex, """
                            move-result p1
                            return p1
                            """
                        )
                    }
                )
            }
        }

        fun addSeekInterfaceMethods(
            targetClass: MutableClass,
            targetMethod: MutableMethod,
            seekMethodName: String,
            methodName: String,
            fieldMethodName: String,
            fieldName: String
        ) {
            targetMethod.apply {
                targetClass.methods.add(
                    ImmutableMethod(
                        definingClass,
                        fieldMethodName,
                        listOf(ImmutableMethodParameter("J", annotations, "time")),
                        "Z",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            4, """
                                # first enum (field a) is SEEK_SOURCE_UNKNOWN
                                sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                                invoke-virtual {p0, p1, p2, v0}, $definingClass->$seekMethodName(J$seekSourceEnumType)Z
                                move-result p1
                                return p1
                                """.toInstructions(),
                            null,
                            null
                        )
                    ).toMutable()
                )

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0, p0, p1}, $definingClass->$fieldMethodName(J)Z
                        move-result v0
                        return v0
                        :ignore
                        const/4 v0, 0x0
                        return v0
                        """

                addStaticFieldToExtension(
                    EXTENSION_CLASS_DESCRIPTOR,
                    methodName,
                    fieldName,
                    definingClass,
                    smaliInstructions
                )
            }
        }

        fun Pair<String, Fingerprint>.getPlayerResponseInstruction(
            returnType: String,
            fromString: Boolean? = null
        ): String {
            methodOrThrow().apply {
                val startIndex = if (fromString == true)
                    matchOrThrow().stringMatches!!.first().index
                else
                    0
                val targetReference = getInstruction<ReferenceInstruction>(
                    indexOfFirstInstructionOrThrow(startIndex) {
                        val reference = getReference<MethodReference>()
                        (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                                reference?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR &&
                                reference.returnType == returnType
                    }
                ).reference

                return "invoke-interface {v$REGISTER_PLAYER_RESPONSE_MODEL}, $targetReference"
            }
        }

        videoEndFingerprint.methodOrThrow().apply {
            findMethodOrThrow(definingClass).let {
                playerConstructorMethod = it
                playerConstructorInsertIndex = it.indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                } + 1
            }

            // hook the player controller for use through extension
            onCreateHook(EXTENSION_CLASS_DESCRIPTOR, "initialize")

            seekSourceEnumType = parameterTypes[1].toString()
            seekSourceMethodName = name

            seekRelativeFingerprint.methodOrThrow(videoEndFingerprint).also { method ->
                seekRelativeSourceMethodName = method.name
                cloneSeekRelativeSourceMethod = method.returnType == "V"
            }

            cloneSeekRelativeSourceMethod(videoEndFingerprint.mutableClassOrThrow())

            // Create extension interface methods.
            addSeekInterfaceMethods(
                videoEndFingerprint.mutableClassOrThrow(),
                this,
                seekSourceMethodName,
                "overrideVideoTime",
                "seekTo",
                "videoInformationClass"
            )
            addSeekInterfaceMethods(
                seekRelativeFingerprint.mutableClassOrThrow(),
                this,
                seekRelativeSourceMethodName,
                "overrideVideoTimeRelative",
                "seekToRelative",
                "videoInformationClass"
            )

            val literalIndex = indexOfFirstLiteralInstructionOrThrow(45368273L)
            val walkerIndex = indexOfFirstInstructionReversedOrThrow(literalIndex) {
                val reference = getReference<MethodReference>()
                (opcode == Opcode.INVOKE_VIRTUAL || opcode == Opcode.INVOKE_VIRTUAL_RANGE) &&
                        reference?.definingClass == definingClass &&
                        reference.parameterTypes.isEmpty() &&
                        reference.returnType == "V"
            }

            videoEndMethod = getWalkerMethod(walkerIndex)
        }

        mdxPlayerDirectorSetVideoStageFingerprint.methodOrThrow().apply {
            findMethodOrThrow(definingClass).let {
                mdxConstructorMethod = it
                mdxConstructorInsertIndex = it.indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                } + 1
            }

            // hook the MDX director for use through extension
            onCreateHookMdx(EXTENSION_CLASS_DESCRIPTOR, "initializeMdx")

            cloneSeekRelativeSourceMethod(mdxPlayerDirectorSetVideoStageFingerprint.mutableClassOrThrow())

            // Create extension interface methods.
            addSeekInterfaceMethods(
                mdxPlayerDirectorSetVideoStageFingerprint.mutableClassOrThrow(),
                this,
                seekSourceMethodName,
                "overrideMDXVideoTime",
                "seekTo",
                "videoInformationMDXClass"
            )
            addSeekInterfaceMethods(
                mdxPlayerDirectorSetVideoStageFingerprint.mutableClassOrThrow(),
                this,
                seekRelativeSourceMethodName,
                "overrideMDXVideoTimeRelative",
                "seekToRelative",
                "videoInformationMDXClass"
            )
        }

        /**
         * Set current video information
         */
        channelIdMethodCall =
            channelIdFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
        channelNameMethodCall =
            channelNameFingerprint.getPlayerResponseInstruction("Ljava/lang/String;", true)
        videoIdMethodCall = videoIdFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
        videoTitleMethodCall =
            videoTitleFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
        videoLengthMethodCall = videoLengthFingerprint.getPlayerResponseInstruction("J")
        videoIsLiveMethodCall = channelIdFingerprint.getPlayerResponseInstruction("Z")

        playbackInitializationFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = indexOfPlayerResponseModelDirectInstruction(this) + 1
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-direct {p0, v$targetRegister}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )

                videoInformationMethod = getVideoInformationMethod()
                it.classDef.methods.add(videoInformationMethod)

                hookVideoInformation("$EXTENSION_CLASS_DESCRIPTOR->setVideoInformation(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
            }
        }

        videoIdFingerprintBackgroundPlay.matchOrThrow().let {
            it.method.apply {
                val targetIndex = indexOfPlayerResponseModelInterfaceInstruction(this)
                val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                addInstruction(
                    targetIndex,
                    "invoke-direct {p0, v$targetRegister}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )

                backgroundVideoInformationMethod = getVideoInformationMethod()
                it.classDef.methods.add(backgroundVideoInformationMethod)
            }
        }

        videoIdFingerprintShorts.matchOrThrow().let {
            it.method.apply {
                val targetIndex = indexOfPlayerResponseModelInterfaceInstruction(this)
                val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                addInstruction(
                    targetIndex,
                    "invoke-direct {p0, v$targetRegister}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )

                shortsVideoInformationMethod = getVideoInformationMethod()
                it.classDef.methods.add(shortsVideoInformationMethod)
            }
        }

        /**
         * Set current video time method
         */
        playerControllerSetTimeReferenceFingerprint.matchOrThrow().let {
            videoTimeConstructorMethod =
                it.getWalkerMethod(it.patternMatch!!.startIndex)
        }

        /**
         * Store receiver (p0) of the time-update method so VOT can use it for volume when playbackSpeedClass is null.
         */
        videoTimeConstructorMethod.addInstruction(
            videoTimeConstructorInsertIndex++,
            "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->setTimeUpdateReceiver(Ljava/lang/Object;)V"
        )
        /**
         * Set current video time
         */
        videoTimeHook(EXTENSION_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Set current video id
         */
        hookVideoId("$EXTENSION_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")
        hookPlayerResponseVideoId(
            "$EXTENSION_CLASS_DESCRIPTOR->setPlayerResponseVideoId(Ljava/lang/String;Z)V"
        )
        // Call before any other video id hooks,
        // so they can use VideoInformation and check if the video id is for a Short.
        addPlayerResponseMethodHook(
            Hook.PlayerParameterBeforeVideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;"
            )
        )

        /**
         * Hook current playback speed
         */
        onPlaybackSpeedItemClickFingerprint.matchOrThrow().let {
            it.method.apply {
                speedSelectionInsertMethod = this
                val speedSelectionValueInstructionIndex =
                    indexOfFirstInstructionOrThrow(Opcode.IGET)

                val setPlaybackSpeedContainerClassFieldIndex =
                    indexOfFirstInstructionReversedOrThrow(
                        speedSelectionValueInstructionIndex,
                        Opcode.IGET_OBJECT
                    )
                val setPlaybackSpeedContainerClassFieldReference =
                    getInstruction<ReferenceInstruction>(setPlaybackSpeedContainerClassFieldIndex).reference

                val setPlaybackSpeedClassFieldReference =
                    getInstruction<ReferenceInstruction>(speedSelectionValueInstructionIndex + 1).reference

                setPlaybackSpeedMethodReference =
                    getInstruction<ReferenceInstruction>(speedSelectionValueInstructionIndex + 2).reference as MethodReference

                // add override playback speed method
                it.classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "overridePlaybackSpeed",
                        listOf(ImmutableMethodParameter("F", annotations, null)),
                        "V",
                        AccessFlags.PUBLIC or AccessFlags.PUBLIC,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            4, """
                                # Check if the playback speed is not auto (-2.0f)
                                const/4 v0, 0x0
                                cmpg-float v0, v3, v0
                                if-lez v0, :ignore
                                
                                # Get the container class field.
                                iget-object v0, v2, $setPlaybackSpeedContainerClassFieldReference  

                                # For some reason, in YouTube 19.44.39 this value is sometimes null.
                                if-eqz v0, :ignore

                                # Get the field from its class.
                                iget-object v1, v0, $setPlaybackSpeedClassFieldReference
                                
                                # Invoke setPlaybackSpeed on that class.
                                invoke-virtual {v1, v3}, $setPlaybackSpeedMethodReference

                                :ignore
                                return-void
                                """.toInstructions(), null, null
                        )
                    ).toMutable()
                )

                // set current playback speed
                val walkerMethod = getWalkerMethod(speedSelectionValueInstructionIndex + 2)
                walkerMethod.apply {
                    addInstruction(
                        this.implementation!!.instructions.size - 1,
                        "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->setPlaybackSpeed(F)V"
                    )
                }
            }
        }

        videoIdFingerprintShorts.matchOrThrow().let {
            it.method.apply {
                val shortsPlaybackSpeedClassField = it.classDef.fields.find { field ->
                    field.type == setPlaybackSpeedMethodReference.definingClass
                } ?: throw PatchException("Failed to find hook field")

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0, p0}, $definingClass->overridePlaybackSpeed(F)V
                        :ignore
                        return-void
                        """

                addStaticFieldToExtension(
                    EXTENSION_CLASS_DESCRIPTOR,
                    "overridePlaybackSpeed",
                    "playbackSpeedShortsClass",
                    definingClass,
                    smaliInstructions
                )

                // add override playback speed method
                it.classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "overridePlaybackSpeed",
                        listOf(ImmutableMethodParameter("F", annotations, null)),
                        "V",
                        AccessFlags.PUBLIC or AccessFlags.PUBLIC,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            3, """
                                # Check if the playback speed is not auto (-2.0f)
                                const/4 v0, 0x0
                                cmpg-float v0, v2, v0
                                if-lez v0, :ignore
                                
                                # Get the container class field.
                                iget-object v0, v1, $shortsPlaybackSpeedClassField  

                                # For some reason, in YouTube 19.44.39 this value is sometimes null.
                                if-eqz v0, :ignore
                                
                                # Invoke setPlaybackSpeed on that class.
                                invoke-virtual {v0, v2}, $setPlaybackSpeedMethodReference

                                :ignore
                                return-void
                                """.toInstructions(), null, null
                        )
                    ).toMutable()
                )
            }
        }

        playbackSpeedClassFingerprint.methodOrThrow().apply {
            val index = indexOfFirstInstructionOrThrow(Opcode.RETURN_OBJECT)
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            val playbackSpeedClass = this.returnType

            // set playback speed class
            addInstructionsAtControlFlowLabel(
                index,
                "sput-object v$register, $EXTENSION_CLASS_DESCRIPTOR->playbackSpeedClass:$playbackSpeedClass"
            )

            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    invoke-virtual {v0, p0}, $playbackSpeedClass->overridePlaybackSpeed(F)V
                    return-void
                    :ignore
                    nop
                    """

            addStaticFieldToExtension(
                EXTENSION_CLASS_DESCRIPTOR,
                "overridePlaybackSpeed",
                "playbackSpeedClass",
                playbackSpeedClass,
                smaliInstructions,
                false
            ).apply {
                val jumpIndex = indexOfFirstInstructionOrThrow(Opcode.NOP)

                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isPlayerInitialized()Z
                        move-result v0
                        if-eqz v0, :ignore
                        """, ExternalLabel("ignore", getInstruction(jumpIndex))
                )
            }

            hookBackgroundPlayVideoInformation("$EXTENSION_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        }

        /**
         * Hook current video quality
         */
        onCreateHook(EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR, "newVideoStarted")

        videoQualityFingerprint.matchOrThrow().let {
            // Fix bad data used by YouTube.
            val (qualityNameField, resolutionField) = with(it.method) {
                val qualityNameIndex = indexOfVideoQualityNameFieldInstruction(this)
                val resolutionIndex = indexOfVideoQualityResolutionFieldInstruction(this)
                val qualityNameReference =
                    getInstruction<ReferenceInstruction>(qualityNameIndex).reference
                val resolutionReference =
                    getInstruction<ReferenceInstruction>(resolutionIndex).reference
                val qualityNameRegister =
                    getInstruction<TwoRegisterInstruction>(qualityNameIndex).registerA
                val resolutionRegister =
                    getInstruction<TwoRegisterInstruction>(resolutionIndex).registerA

                addInstructions(
                    0, """
                        invoke-static { v$qualityNameRegister, v$resolutionRegister }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->fixVideoQualityResolution(Ljava/lang/String;I)I
                        move-result v$resolutionRegister
                        """
                )

                Pair(
                    qualityNameReference,
                    resolutionReference
                )
            }

            // Add methods to access obfuscated quality fields.
            it.classDef.apply {
                methods.add(
                    ImmutableMethod(
                        type,
                        "patch_getQualityName",
                        listOf(),
                        "Ljava/lang/String;",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        addInstructions(
                            0, """
                                iget-object v0, p0, $qualityNameField
                                return-object v0
                                """
                        )
                    }
                )

                methods.add(
                    ImmutableMethod(
                        type,
                        "patch_getResolution",
                        listOf(),
                        "I",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        addInstructions(
                            0, """
                                iget v0, p0, $resolutionField
                                return v0
                                """
                        )
                    }
                )
            }
        }

        val formatStreamFpsReference = formatStreamingModelQualityLabelBuilderFingerprint
            .matchOrThrow()
            .let {
                with(it.method) {
                    val stringIndex = it.stringMatches!!.first().index
                    val formatStreamIndex = indexOfFirstInstructionReversedOrThrow(stringIndex) {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                reference?.definingClass == YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE &&
                                reference.parameterTypes.isEmpty() &&
                                reference.returnType == "I"
                    }
                    getInstruction<ReferenceInstruction>(formatStreamIndex).reference
                }
            }

        val formatStreamQualityNameReference = formatStreamingModelQualityLabelBuilderFingerprint
            .methodOrThrow()
            .methodCall()

        val formatStreamITagReference =
            formatStreamModelToStringFingerprint.originalMethodOrThrow()
                .findMethodFromToString("FormatStream(itag=")
                .methodCall()

        val formatStreamResolutionReference =
            availableVideoFormatsFingerprint.matchOrThrow(
                formatStreamModelBuilderFingerprint
            ).let {
                with(it.method) {
                    val formatStreamIndex = it.patternMatch!!.startIndex + 1
                    val formatStreamResolutionReference =
                        getInstruction<ReferenceInstruction>(formatStreamIndex).reference as MethodReference

                    addInstructions(
                        0,
                        "invoke-static { p0 }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoFormat(Ljava/util/List;)V"
                    )

                    formatStreamResolutionReference
                }
            }

        mapOf(
            formatStreamFpsReference to "patch_getFps",
            formatStreamITagReference to "patch_getITag",
            formatStreamResolutionReference to "patch_getResolution",
            formatStreamQualityNameReference to "patch_getQualityName"
        ).forEach { (reference, methodName) ->
            val returnTypeIsInteger = reference.toString().endsWith("I")
            val returnType = if (returnTypeIsInteger) "I" else "Ljava/lang/String;"
            val smaliInstructions = if (returnTypeIsInteger)
                """
                    invoke-virtual { p0 }, $reference
                    move-result v0
                    return v0
                """
            else
                """
                    invoke-virtual { p0 }, $reference
                    move-result-object v0
                    return-object v0
                """
            findMutableClassOrThrow(YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE)
                .methods.add(
                    ImmutableMethod(
                        YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE,
                        methodName,
                        listOf(),
                        returnType,
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            smaliInstructions
                        )
                    }
                )
        }

        initFormatStreamFingerprint.methodOrThrow(initFormatStreamParentFingerprint)
            .apply {
                val preferredFormatStreamIndex =
                    indexOfPreferredFormatStreamInstruction(this)
                val preferredFormatStreamReference =
                    getInstruction<ReferenceInstruction>(preferredFormatStreamIndex).reference
                val preferredFormatStreamInstruction =
                    getInstruction<TwoRegisterInstruction>(preferredFormatStreamIndex)
                val preferredFormatStreamRegister =
                    preferredFormatStreamInstruction.registerA
                val definingClassRegister =
                    preferredFormatStreamInstruction.registerB

                addInstructions(
                    preferredFormatStreamIndex + 1, """
                        invoke-static { v$preferredFormatStreamRegister }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->getVideoFormat($YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE)$YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE
                        move-result-object v$preferredFormatStreamRegister
                        iput-object v$preferredFormatStreamRegister, v$definingClassRegister, $preferredFormatStreamReference
                        """
                )
            }

        val initialResolutionField =
            playbackStartParametersToStringFingerprint.originalMethodOrThrow()
                .findFieldFromToString(FIXED_RESOLUTION_STRING)

        playbackStartParametersConstructorFingerprint
            .methodOrThrow(playbackStartParametersToStringFingerprint)
            .apply {
                val index = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>() == initialResolutionField
                }
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->getInitialVideoQuality(Lj${'$'}/util/Optional;)Lj${'$'}/util/Optional;
                        move-result-object v$register
                        """
                )
            }

        videoQualityArrayFingerprint.matchOrThrow(formatStreamModelBuilderFingerprint).let {
            it.method.apply {
                val index = it.patternMatch!!.startIndex
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructionsAtControlFlowLabel(
                    index, """
                        invoke-static { v$register }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->removeLowFpsVideoQualities(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$register
                        """
                )
            }
        }

        videoQualityListFingerprint.matchOrThrow().let {
            val classDef = it.classDef
            it.method.apply {
                classDef.interfaces.add(EXTENSION_VIDEO_QUALITY_MENU_INTERFACE)

                classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "patch_setQuality",
                        listOf(
                            ImmutableMethodParameter(
                                YOUTUBE_VIDEO_QUALITY_CLASS_TYPE,
                                annotations,
                                null
                            )
                        ),
                        "V",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        val setQualityMenuIndexMethod = classDef.methods.single { method ->
                            method.parameterTypes.firstOrNull() == YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
                        }

                        addInstructions(
                            0,
                            """
                                invoke-virtual { p0, p1 }, $setQualityMenuIndexMethod
                                return-void
                                """
                        )
                    }
                )
                val interfaceIndex = it.patternMatch!!.startIndex
                val listRegister =
                    getInstruction<FiveRegisterInstruction>(interfaceIndex).registerD
                val indexRegister =
                    getInstruction<FiveRegisterInstruction>(interfaceIndex).registerE

                addInstructions(
                    interfaceIndex, """
                        invoke-static { v$listRegister, p0, v$indexRegister }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQuality([$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE${EXTENSION_VIDEO_QUALITY_MENU_INTERFACE}I)I
                        move-result v$indexRegister
                        """
                )
            }
        }

        videoQualitySetterFingerprint.matchOrThrow().let {
            it.method.apply {
                val textIndex = it.patternMatch!!.endIndex
                val textRegister = getInstruction<TwoRegisterInstruction>(textIndex).registerA

                addInstruction(
                    textIndex + 1,
                    "invoke-static {v$textRegister}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->updateQualityString(Ljava/lang/String;)V"
                )
            }
        }
    }
}

private fun MutableMethod.getVideoInformationMethod(): MutableMethod =
    ImmutableMethod(
        definingClass,
        "setVideoInformation",
        listOf(
            ImmutableMethodParameter(
                PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR,
                annotations,
                null
            )
        ),
        "V",
        AccessFlags.PRIVATE or AccessFlags.FINAL,
        annotations,
        null,
        ImmutableMethodImplementation(
            REGISTER_PLAYER_RESPONSE_MODEL + 1, """
                $channelIdMethodCall
                move-result-object v$REGISTER_CHANNEL_ID
                $channelNameMethodCall
                move-result-object v$REGISTER_CHANNEL_NAME
                $videoIdMethodCall
                move-result-object v$REGISTER_VIDEO_ID
                $videoTitleMethodCall
                move-result-object v$REGISTER_VIDEO_TITLE
                $videoLengthMethodCall
                move-result-wide v$REGISTER_VIDEO_LENGTH
                $videoIsLiveMethodCall
                move-result v$REGISTER_VIDEO_IS_LIVE
                return-void
                """.toInstructions(),
            null,
            null
        )
    ).toMutable()

private fun MutableMethod.insert(insertIndex: Int, register: String, descriptor: String) =
    addInstruction(insertIndex, "invoke-static/range { $register }, $descriptor")

/**
 * Hook the player controller.  Called when a video is opened or the current video is changed.
 *
 * Note: This hook is called very early and is called before the video id, video time, video length,
 * and many other data fields are set.
 *
 * @param targetMethodClass The descriptor for the class to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun onCreateHook(targetMethodClass: String, targetMethodName: String) =
    playerConstructorMethod.addInstruction(
        playerConstructorInsertIndex++,
        "invoke-static { }, $targetMethodClass->$targetMethodName()V"
    )

/**
 * Hook the MDX player director. Called when playing videos while casting to a big screen device.
 *
 * @param targetMethodClass The descriptor for the class to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun onCreateHookMdx(targetMethodClass: String, targetMethodName: String) =
    mdxConstructorMethod.addInstruction(
        mdxConstructorInsertIndex++,
        "invoke-static { }, $targetMethodClass->$targetMethodName()V"
    )

/**
 * Hook the video time.
 * The hook is usually called once per second.
 *
 * @param targetMethodClass The descriptor for the static method to invoke when the player controller is created.
 * @param targetMethodName The name of the static method to invoke when the player controller is created.
 */
internal fun videoTimeHook(targetMethodClass: String, targetMethodName: String) =
    videoTimeConstructorMethod.addInstruction(
        videoTimeConstructorInsertIndex++,
        "invoke-static { p1, p2 }, $targetMethodClass->$targetMethodName(J)V"
    )

/**
 * This method is invoked on both regular videos and Shorts.
 */
internal fun hookVideoInformation(descriptor: String) =
    videoInformationMethod.apply {
        val index = implementation!!.instructions.lastIndex

        insert(
            index,
            "v$REGISTER_CHANNEL_ID .. v$REGISTER_VIDEO_IS_LIVE",
            descriptor
        )
    }

/**
 * This method is invoked only in regular videos.
 */
internal fun hookBackgroundPlayVideoInformation(descriptor: String) =
    backgroundVideoInformationMethod.apply {
        val index = implementation!!.instructions.lastIndex

        insert(
            index,
            "v$REGISTER_CHANNEL_ID .. v$REGISTER_VIDEO_IS_LIVE",
            descriptor
        )
    }

/**
 * This method is invoked only in shorts videos.
 */
internal fun hookShortsVideoInformation(descriptor: String) =
    shortsVideoInformationMethod.apply {
        val index = implementation!!.instructions.lastIndex

        insert(
            index,
            "v$REGISTER_CHANNEL_ID .. v$REGISTER_VIDEO_IS_LIVE",
            descriptor
        )
    }
