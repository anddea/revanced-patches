package app.revanced.patches.youtube.video.information

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.shared.mdxPlayerDirectorSetVideoStageFingerprint
import app.revanced.patches.shared.videoLengthFingerprint
import app.revanced.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.SHARED_PATH
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.videoEndFingerprint
import app.revanced.patches.youtube.video.playerresponse.Hook
import app.revanced.patches.youtube.video.playerresponse.addPlayerResponseMethodHook
import app.revanced.patches.youtube.video.playerresponse.playerResponseMethodHookPatch
import app.revanced.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.revanced.patches.youtube.video.videoid.hookVideoId
import app.revanced.patches.youtube.video.videoid.videoIdPatch
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.cloneMutable
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/VideoInformation;"

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

        fun Pair<String, Fingerprint>.getPlayerResponseInstruction(returnType: String, fromString: Boolean? = null): String {
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
            val walkerIndex =
                indexOfFirstInstructionReversedOrThrow(
                    literalIndex,
                    Opcode.INVOKE_VIRTUAL_RANGE
                )

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
                    getInstruction<ReferenceInstruction>(setPlaybackSpeedContainerClassFieldIndex).reference.toString()

                val setPlaybackSpeedClassFieldReference =
                    getInstruction<ReferenceInstruction>(speedSelectionValueInstructionIndex + 1).reference.toString()
                val setPlaybackSpeedMethodReference =
                    getInstruction<ReferenceInstruction>(speedSelectionValueInstructionIndex + 2).reference.toString()

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

        playbackSpeedClassFingerprint.matchOrThrow().let { result ->
            result.method.apply {
                val index = result.patternMatch!!.endIndex
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                val playbackSpeedClass = this.returnType

                // set playback speed class
                replaceInstruction(
                    index,
                    "sput-object v$register, $EXTENSION_CLASS_DESCRIPTOR->playbackSpeedClass:$playbackSpeedClass"
                )
                addInstruction(
                    index + 1,
                    "return-object v$register"
                )

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0, p0}, $playbackSpeedClass->overridePlaybackSpeed(F)V
                        :ignore
                        return-void
                    """

                addStaticFieldToExtension(
                    EXTENSION_CLASS_DESCRIPTOR,
                    "overridePlaybackSpeed",
                    "playbackSpeedClass",
                    playbackSpeedClass,
                    smaliInstructions,
                    false
                )
            }
        }

        /**
         * Hook current video quality
         */
        videoQualityListFingerprint.matchOrThrow().let {
            val overrideMethod =
                it.classDef.methods.find { method -> method.parameterTypes.first() == "I" }

            val videoQualityClass = it.method.definingClass
            val videoQualityMethodName = overrideMethod?.name
                ?: throw PatchException("Failed to find hook method")

            // set video quality array
            it.method.apply {
                val listIndex = it.patternMatch!!.startIndex
                val listRegister = getInstruction<FiveRegisterInstruction>(listIndex).registerD

                addInstruction(
                    listIndex,
                    "invoke-static {v$listRegister}, $EXTENSION_CLASS_DESCRIPTOR->setVideoQualityList([Ljava/lang/Object;)V"
                )
            }

            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    invoke-virtual {v0, p0}, $videoQualityClass->$videoQualityMethodName(I)V
                    :ignore
                    return-void
                """

            addStaticFieldToExtension(
                EXTENSION_CLASS_DESCRIPTOR,
                "overrideVideoQuality",
                "videoQualityClass",
                videoQualityClass,
                smaliInstructions
            )
        }

        // set current video quality
        videoQualityTextFingerprint.matchOrThrow().let {
            it.method.apply {
                val textIndex = it.patternMatch!!.endIndex
                val textRegister = getInstruction<TwoRegisterInstruction>(textIndex).registerA

                addInstruction(
                    textIndex + 1,
                    "invoke-static {v$textRegister}, $EXTENSION_CLASS_DESCRIPTOR->setVideoQuality(Ljava/lang/String;)V"
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