package app.revanced.patches.music.video.information

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.music.utils.integrations.Constants.SHARED_PATH
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.video.information.fingerprints.PlaybackSpeedFingerprint
import app.revanced.patches.music.video.information.fingerprints.PlaybackSpeedParentFingerprint
import app.revanced.patches.music.video.information.fingerprints.PlayerControllerSetTimeReferenceFingerprint
import app.revanced.patches.music.video.information.fingerprints.VideoEndFingerprint
import app.revanced.patches.music.video.information.fingerprints.VideoIdFingerprint
import app.revanced.patches.music.video.information.fingerprints.VideoQualityListFingerprint
import app.revanced.patches.music.video.information.fingerprints.VideoQualityTextFingerprint
import app.revanced.patches.shared.fingerprints.MdxPlayerDirectorSetVideoStageFingerprint
import app.revanced.patches.shared.fingerprints.VideoLengthFingerprint
import app.revanced.util.addFieldAndInstructions
import app.revanced.util.alsoResolve
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

@Patch(
    dependencies = [SharedResourceIdPatch::class]
)
@Suppress("MemberVisibilityCanBePrivate")
object VideoInformationPatch : BytecodePatch(
    setOf(
        MdxPlayerDirectorSetVideoStageFingerprint,
        PlayerControllerSetTimeReferenceFingerprint,
        PlaybackSpeedParentFingerprint,
        VideoEndFingerprint,
        VideoIdFingerprint,
        VideoLengthFingerprint,
        VideoQualityListFingerprint,
        VideoQualityTextFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$SHARED_PATH/VideoInformation;"

    private const val REGISTER_PLAYER_RESPONSE_MODEL = 4

    private const val REGISTER_VIDEO_ID = 0
    private const val REGISTER_VIDEO_LENGTH = 1

    @Suppress("unused")
    private const val REGISTER_VIDEO_LENGTH_DUMMY = 2

    private lateinit var PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR: String
    private lateinit var videoIdMethodCall: String
    private lateinit var videoLengthMethodCall: String

    private lateinit var videoInformationMethod: MutableMethod

    /**
     * Used in [VideoEndFingerprint] and [MdxPlayerDirectorSetVideoStageFingerprint].
     * Since both classes are inherited from the same class,
     * [VideoEndFingerprint] and [MdxPlayerDirectorSetVideoStageFingerprint] always have the same [seekSourceEnumType] and [seekSourceMethodName].
     */
    private var seekSourceEnumType = ""
    private var seekSourceMethodName = ""

    private lateinit var videoInformationMutableClass: MutableClass
    private lateinit var context: BytecodeContext

    private lateinit var playerConstructorMethod: MutableMethod
    private var playerConstructorInsertIndex = -1

    private lateinit var mdxConstructorMethod: MutableMethod
    private var mdxConstructorInsertIndex = -1

    private lateinit var videoTimeConstructorMethod: MutableMethod
    private var videoTimeConstructorInsertIndex = 2

    // Used by other patches.
    internal lateinit var playbackSpeedResult: MethodFingerprintResult

    private fun addSeekInterfaceMethods(
        result: MethodFingerprintResult,
        seekMethodName: String,
        methodName: String,
        fieldName: String
    ) {
        result.mutableMethod.apply {
            result.mutableClass.methods.add(
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
                    invoke-virtual {v0, p0, p1}, $definingClass->seekTo(J)Z
                    move-result v0
                    return v0
                    :ignore
                    const/4 v0, 0x0
                    return v0
                    """

            videoInformationMutableClass.addFieldAndInstructions(
                context,
                methodName,
                fieldName,
                definingClass,
                smaliInstructions,
                true
            )
        }
    }

    override fun execute(context: BytecodeContext) {
        this.context = context
        videoInformationMutableClass =
            context.findClass(INTEGRATIONS_CLASS_DESCRIPTOR)!!.mutableClass

        VideoEndFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                playerConstructorMethod =
                    it.mutableClass.methods.first { method -> MethodUtil.isConstructor(method) }

                playerConstructorInsertIndex =
                    playerConstructorMethod.indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                    } + 1

                // hook the player controller for use through integrations
                onCreateHook(INTEGRATIONS_CLASS_DESCRIPTOR, "initialize")

                seekSourceEnumType = parameterTypes[1].toString()
                seekSourceMethodName = name

                // Create integrations interface methods.
                addSeekInterfaceMethods(
                    it,
                    seekSourceMethodName,
                    "overrideVideoTime",
                    "videoInformationClass"
                )
            }
        }

        MdxPlayerDirectorSetVideoStageFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                mdxConstructorMethod =
                    it.mutableClass.methods.first { method -> MethodUtil.isConstructor(method) }

                mdxConstructorInsertIndex = mdxConstructorMethod.indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                } + 1

                // hook the MDX director for use through integrations
                onCreateHookMdx(INTEGRATIONS_CLASS_DESCRIPTOR, "initializeMdx")

                // Create integrations interface methods.
                addSeekInterfaceMethods(
                    it,
                    seekSourceMethodName,
                    "overrideMDXVideoTime",
                    "videoInformationMDXClass"
                )
            }
        }

        /**
         * Set current video information
         */
        VideoIdFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val playerResponseModelIndex = it.scanResult.patternScanResult!!.startIndex

                PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR =
                    getInstruction(playerResponseModelIndex)
                    .getReference<MethodReference>()
                    ?.definingClass
                    ?: throw PatchException("Could not find Player Response Model class")

                videoIdMethodCall =
                    VideoIdFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
                videoLengthMethodCall =
                    VideoLengthFingerprint.getPlayerResponseInstruction("J")

                videoInformationMethod = getVideoInformationMethod()
                it.mutableClass.methods.add(videoInformationMethod)

                addInstruction(
                    playerResponseModelIndex + 2,
                    "invoke-direct/range {p0 .. p1}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )
            }
        }

        /**
         * Set the video time method
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
         * Set current video length
         */
        videoLengthHook("$INTEGRATIONS_CLASS_DESCRIPTOR->setVideoLength(J)V")

        /**
         * Set current video id
         */
        videoIdHook("$INTEGRATIONS_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")

        /**
         * Hook current playback speed
         */
        PlaybackSpeedFingerprint.alsoResolve(
            context, PlaybackSpeedParentFingerprint
        ).let {
            it.mutableMethod.apply {
                playbackSpeedResult = it
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val speedMethod = getWalkerMethod(context, endIndex)

                // set current playback speed
                speedMethod.addInstruction(
                    speedMethod.implementation!!.instructions.size - 1,
                    "invoke-static {p1}, $INTEGRATIONS_CLASS_DESCRIPTOR->setPlaybackSpeed(F)V"
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

    private fun MethodFingerprint.getPlayerResponseInstruction(returnType: String): String {
        resultOrThrow().mutableMethod.apply {
            val targetReference = getInstruction<ReferenceInstruction>(
                indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                            reference?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR &&
                            reference.returnType == returnType
                }
            ).reference

            return "invoke-interface/range {v$REGISTER_PLAYER_RESPONSE_MODEL .. v$REGISTER_PLAYER_RESPONSE_MODEL}, $targetReference"
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
                    $videoIdMethodCall
                    move-result-object v$REGISTER_VIDEO_ID
                    $videoLengthMethodCall
                    move-result-wide v$REGISTER_VIDEO_LENGTH
                    return-void
                    """.toInstructions(),
                null,
                null
            )
        ).toMutable()

    private fun MutableMethod.insert(insertIndex: Int, register: String, descriptor: String) =
        addInstruction(insertIndex, "invoke-static { $register }, $descriptor")

    private fun MutableMethod.insertTimeHook(insertIndex: Int, descriptor: String) =
        insert(insertIndex, "p1, p2", descriptor)

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

    internal fun videoIdHook(
        descriptor: String
    ) = videoInformationMethod.apply {
        addInstruction(
            implementation!!.instructions.lastIndex,
            "invoke-static {v$REGISTER_VIDEO_ID}, $descriptor"
        )
    }

    internal fun videoLengthHook(
        descriptor: String
    ) = videoInformationMethod.apply {
        addInstruction(
            implementation!!.instructions.lastIndex,
            "invoke-static {v$REGISTER_VIDEO_LENGTH, v$REGISTER_VIDEO_LENGTH_DUMMY}, $descriptor"
        )
    }

    /**
     * Hook the video time.
     * The hook is usually called once per second.
     *
     * @param targetMethodClass The descriptor for the static method to invoke when the player controller is created.
     * @param targetMethodName The name of the static method to invoke when the player controller is created.
     */
    internal fun videoTimeHook(targetMethodClass: String, targetMethodName: String) =
        videoTimeConstructorMethod.insertTimeHook(
            videoTimeConstructorInsertIndex++,
            "$targetMethodClass->$targetMethodName(J)V"
        )
}