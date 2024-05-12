package app.revanced.patches.youtube.video.information

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.youtube.utils.fingerprints.OrganicPlaybackContextModelFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoEndFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHARED_PATH
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.video.information.fingerprints.ChannelIdFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.ChannelNameFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.OnPlaybackSpeedItemClickFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.PlaybackInitializationFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.PlaybackSpeedClassFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.PlayerControllerSetTimeReferenceFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.VideoIdFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.VideoIdFingerprintBackgroundPlay
import app.revanced.patches.youtube.video.information.fingerprints.VideoIdFingerprintShorts
import app.revanced.patches.youtube.video.information.fingerprints.VideoLengthFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.VideoQualityListFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.VideoQualityTextFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.VideoTitleFingerprint
import app.revanced.patches.youtube.video.playerresponse.PlayerResponseMethodHookPatch
import app.revanced.patches.youtube.video.videoid.VideoIdPatch
import app.revanced.util.addFieldAndInstructions
import app.revanced.util.getReference
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.resultOrThrow
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
import com.android.tools.smali.dexlib2.util.MethodUtil

@Patch(
    description = "Hooks YouTube to get information about the current playing video.",
    dependencies = [
        PlayerResponseMethodHookPatch::class,
        PlayerTypeHookPatch::class,
        SharedResourceIdPatch::class,
        VideoIdPatch::class
    ]
)
object VideoInformationPatch : BytecodePatch(
    setOf(
        ChannelIdFingerprint,
        ChannelNameFingerprint,
        OnPlaybackSpeedItemClickFingerprint,
        OrganicPlaybackContextModelFingerprint,
        PlaybackInitializationFingerprint,
        PlaybackSpeedClassFingerprint,
        PlayerControllerSetTimeReferenceFingerprint,
        VideoEndFingerprint,
        VideoIdFingerprint,
        VideoIdFingerprintBackgroundPlay,
        VideoIdFingerprintShorts,
        VideoLengthFingerprint,
        VideoQualityListFingerprint,
        VideoQualityTextFingerprint,
        VideoTitleFingerprint,
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$SHARED_PATH/VideoInformation;"

    private const val PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR =
        "Lcom/google/android/libraries/youtube/innertube/model/player/PlayerResponseModel;"

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

    private lateinit var playerConstructorMethod: MutableMethod
    private var playerConstructorInsertIndex = 4

    private lateinit var videoTimeConstructorMethod: MutableMethod
    private var videoTimeConstructorInsertIndex = 2

    private lateinit var videoTimeMethod: MutableMethod
    private var videoTimeIndex = 1

    // Used by other patches.
    internal lateinit var speedSelectionInsertMethod: MutableMethod
    internal lateinit var videoEndMethod: MutableMethod

    override fun execute(context: BytecodeContext) {
        val videoInformationMutableClass = context.findClass(INTEGRATIONS_CLASS_DESCRIPTOR)!!.mutableClass

        VideoEndFingerprint.resultOrThrow().let {

            playerConstructorMethod =
                it.mutableClass.methods.first { method -> MethodUtil.isConstructor(method) }

            // hook the player controller for use through integrations
            onCreateHook(INTEGRATIONS_CLASS_DESCRIPTOR, "initialize")

            it.mutableMethod.apply {
                val seekSourceEnumType = parameterTypes[1].toString()

                it.mutableClass.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "seekTo",
                        listOf(ImmutableMethodParameter("J", annotations, "time")),
                        "Z",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            4, """
                                sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                                invoke-virtual {p0, p1, p2, v0}, ${definingClass}->${name}(J$seekSourceEnumType)Z
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
                        invoke-virtual {v0, p0, p1}, $definingClass->seekTo(J)Z
                        move-result v0
                        return v0
                        :ignore
                        const/4 v0, 0x0
                        return v0
                    """

                videoInformationMutableClass.addFieldAndInstructions(
                    context,
                    "overrideVideoTime",
                    "videoInformationClass",
                    definingClass,
                    smaliInstructions,
                    true
                )

                videoEndMethod = getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex + 1)
            }
        }

        /**
         * Set current video information
         */
        channelIdMethodCall = ChannelIdFingerprint.getMethodName("Ljava/lang/String;")
        channelNameMethodCall = ChannelNameFingerprint.getMethodName("Ljava/lang/String;")
        videoIdMethodCall = VideoIdFingerprint.getMethodName("Ljava/lang/String;")
        videoTitleMethodCall = VideoTitleFingerprint.getMethodName("Ljava/lang/String;")
        videoLengthMethodCall = VideoLengthFingerprint.getMethodName("J")
        videoIsLiveMethodCall = ChannelIdFingerprint.getMethodName("Z")

        PlaybackInitializationFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_DIRECT
                            && getReference<MethodReference>()?.returnType == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
                } + 1
                if (targetIndex == 0) throw PatchException("Could not find instruction index.")
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-direct {p0, v$targetRegister}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )

                videoInformationMethod = getVideoInformationMethod()
                it.mutableClass.methods.add(videoInformationMethod)

                hook("$INTEGRATIONS_CLASS_DESCRIPTOR->setVideoInformation(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
            }
        }

        VideoIdFingerprintBackgroundPlay.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_INTERFACE
                            && getReference<MethodReference>()?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
                }
                if (targetIndex < 0) throw PatchException("Could not find instruction index.")
                val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                addInstruction(
                    targetIndex,
                    "invoke-direct {p0, v$targetRegister}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )

                backgroundVideoInformationMethod = getVideoInformationMethod()
                it.mutableClass.methods.add(backgroundVideoInformationMethod)
            }
        }

        VideoIdFingerprintShorts.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_INTERFACE
                            && getReference<MethodReference>()?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
                }
                if (targetIndex < 0) throw PatchException("Could not find instruction index.")
                val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                addInstruction(
                    targetIndex,
                    "invoke-direct {p0, v$targetRegister}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )

                shortsVideoInformationMethod = getVideoInformationMethod()
                it.mutableClass.methods.add(shortsVideoInformationMethod)
            }
        }

        /**
         * Set current video time method
         */
        PlayerControllerSetTimeReferenceFingerprint.resultOrThrow().let {
            videoTimeConstructorMethod =
                it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)
        }

        /**
         * Set current video time
         */
        videoTimeHook(INTEGRATIONS_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Set current video id
         */
        VideoIdPatch.hookPlayerResponseVideoId(
            "$INTEGRATIONS_CLASS_DESCRIPTOR->setPlayerResponseVideoId(Ljava/lang/String;Z)V")
        // Call before any other video id hooks,
        // so they can use VideoInformation and check if the video id is for a Short.
        PlayerResponseMethodHookPatch += PlayerResponseMethodHookPatch.Hook.PlayerParameterBeforeVideoId(
            "$INTEGRATIONS_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;")

        /**
         * Hook current playback speed
         */
        OnPlaybackSpeedItemClickFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                speedSelectionInsertMethod = this
                val speedSelectionValueInstructionIndex = getTargetIndex(Opcode.IGET)

                val setPlaybackSpeedContainerClassFieldIndex = getTargetIndexReversed(speedSelectionValueInstructionIndex, Opcode.IGET_OBJECT)
                val setPlaybackSpeedContainerClassFieldReference =
                    getInstruction<ReferenceInstruction>(setPlaybackSpeedContainerClassFieldIndex).reference.toString()

                val setPlaybackSpeedClassFieldReference =
                    getInstruction<ReferenceInstruction>(speedSelectionValueInstructionIndex + 1).reference.toString()
                val setPlaybackSpeedMethodReference =
                    getInstruction<ReferenceInstruction>(speedSelectionValueInstructionIndex + 2).reference.toString()

                // add override playback speed method
                it.mutableClass.methods.add(
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
                val walkerMethod = getWalkerMethod(context, speedSelectionValueInstructionIndex + 2)
                walkerMethod.apply {
                    addInstruction(
                        this.implementation!!.instructions.size - 1,
                        "invoke-static { p1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->setPlaybackSpeed(F)V"
                    )
                }
            }
        }

        PlaybackSpeedClassFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val index = result.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                val playbackSpeedClass = this.returnType

                // set playback speed class
                replaceInstruction(
                    index,
                    "sput-object v$register, $INTEGRATIONS_CLASS_DESCRIPTOR->playbackSpeedClass:$playbackSpeedClass"
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

                videoInformationMutableClass.addFieldAndInstructions(
                    context,
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
        VideoQualityListFingerprint.resultOrThrow().let {
            val overrideMethod =
                it.mutableClass.methods.find { method -> method.parameterTypes.first() == "I" }

            val videoQualityClass = it.method.definingClass
            val videoQualityMethodName = overrideMethod?.name
                ?: throw PatchException("Failed to find hook method")

            // set video quality array
            it.mutableMethod.apply {
                val listIndex = it.scanResult.patternScanResult!!.startIndex
                val listRegister = getInstruction<FiveRegisterInstruction>(listIndex).registerD

                addInstruction(
                    listIndex,
                    "invoke-static {v$listRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setVideoQualityList([Ljava/lang/Object;)V"
                )
            }

            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    invoke-virtual {v0, p0}, $videoQualityClass->$videoQualityMethodName(I)V
                    :ignore
                    return-void
                """

            videoInformationMutableClass.addFieldAndInstructions(
                context,
                "overrideVideoQuality",
                "videoQualityClass",
                videoQualityClass,
                smaliInstructions,
                true
            )
        }

        // set current video quality
        VideoQualityTextFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val textIndex = it.scanResult.patternScanResult!!.endIndex
                val textRegister = getInstruction<TwoRegisterInstruction>(textIndex).registerA

                addInstruction(
                    textIndex + 1,
                    "invoke-static {v$textRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setVideoQuality(Ljava/lang/String;)V"
                )
            }
        }
    }

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
            "invoke-static {}, $targetMethodClass->$targetMethodName()V"
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

    private fun MethodFingerprint.getMethodName(returnType : String) :String {
        resultOrThrow().mutableMethod.apply {
            val targetIndex = indexOfFirstInstruction {
                opcode == Opcode.INVOKE_INTERFACE
                        && getReference<MethodReference>()?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
                        && getReference<MethodReference>()?.returnType == returnType
            }
            if (targetIndex < 0) throw PatchException("Could not find instruction index.")
            val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference

            return "invoke-interface {v${REGISTER_PLAYER_RESPONSE_MODEL}}, $targetReference"
        }
    }

    private fun MutableMethod.getVideoInformationMethod(): MutableMethod =
        ImmutableMethod(
            definingClass,
            "setVideoInformation",
            listOf(ImmutableMethodParameter(PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR, annotations, null)),
            "V",
            AccessFlags.PRIVATE or AccessFlags.FINAL,
            annotations,
            null,
            ImmutableMethodImplementation(
                9, """
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
     * This method is invoked on both regular videos and Shorts.
     */
    internal fun hook(descriptor: String) =
        videoInformationMethod.apply {
            val index = implementation!!.instructions.size - 1

            insert(
                index,
                "v${REGISTER_CHANNEL_ID} .. v${REGISTER_VIDEO_IS_LIVE}",
                descriptor
            )
        }

    /**
     * This method is invoked only in regular videos.
     */
    internal fun hookBackgroundPlay(descriptor: String) =
        backgroundVideoInformationMethod.apply {
            val index = implementation!!.instructions.size - 1

            insert(
                index,
                "v${REGISTER_CHANNEL_ID} .. v${REGISTER_VIDEO_IS_LIVE}",
                descriptor
            )
        }

    /**
     * This method is invoked only in shorts videos.
     */
    internal fun hookShorts(descriptor: String) =
        shortsVideoInformationMethod.apply {
            val index = implementation!!.instructions.size - 1

            insert(
                index,
                "v${REGISTER_CHANNEL_ID} .. v${REGISTER_VIDEO_IS_LIVE}",
                descriptor
            )
        }
}