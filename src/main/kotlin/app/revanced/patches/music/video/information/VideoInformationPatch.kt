package app.revanced.patches.music.video.information

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.music.utils.fingerprints.SeekBarConstructorFingerprint
import app.revanced.patches.music.utils.integrations.Constants.SHARED_PATH
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.video.information.fingerprints.PlaybackSpeedFingerprint
import app.revanced.patches.music.video.information.fingerprints.PlaybackSpeedParentFingerprint
import app.revanced.patches.music.video.information.fingerprints.PlayerControllerSetTimeReferenceFingerprint
import app.revanced.patches.music.video.information.fingerprints.VideoEndFingerprint
import app.revanced.patches.music.video.information.fingerprints.VideoLengthFingerprint
import app.revanced.patches.music.video.information.fingerprints.VideoQualityListFingerprint
import app.revanced.patches.music.video.information.fingerprints.VideoQualityTextFingerprint
import app.revanced.patches.music.video.videoid.VideoIdPatch
import app.revanced.util.addFieldAndInstructions
import app.revanced.util.getTargetIndexWithFieldReferenceTypeReversed
import app.revanced.util.getTargetIndexWithMethodReferenceNameReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Patch(
    dependencies = [
        SharedResourceIdPatch::class,
        VideoIdPatch::class
    ]
)
@Suppress("MemberVisibilityCanBePrivate")
object VideoInformationPatch : BytecodePatch(
    setOf(
        PlayerControllerSetTimeReferenceFingerprint,
        PlaybackSpeedParentFingerprint,
        SeekBarConstructorFingerprint,
        VideoEndFingerprint,
        VideoQualityListFingerprint,
        VideoQualityTextFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$SHARED_PATH/VideoInformation;"

    private lateinit var videoTimeConstructorMethod: MutableMethod
    private var videoTimeConstructorInsertIndex = 2

    // Used by other patches.
    lateinit var rectangleFieldName: String
    internal lateinit var playbackSpeedResult: MethodFingerprintResult

    override fun execute(context: BytecodeContext) {
        val videoInformationMutableClass = context.findClass(INTEGRATIONS_CLASS_DESCRIPTOR)!!.mutableClass

        VideoEndFingerprint.resultOrThrow().let {
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
            }
        }

        /**
         * Set the video time method
         */
        PlayerControllerSetTimeReferenceFingerprint.resultOrThrow().let {
            videoTimeConstructorMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)
        }

        /**
         * Set current video time
         */
        videoTimeHook(INTEGRATIONS_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Set current video length
         */
        VideoLengthFingerprint.resolve(
            context,
            SeekBarConstructorFingerprint.resultOrThrow().classDef
        )
        VideoLengthFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val invalidateIndex = getTargetIndexWithMethodReferenceNameReversed("invalidate")
                val rectangleIndex = getTargetIndexWithFieldReferenceTypeReversed(invalidateIndex + 1, "Landroid/graphics/Rect;")
                rectangleFieldName = (getInstruction<ReferenceInstruction>(rectangleIndex).reference as FieldReference).name

                val videoLengthRegisterIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val videoLengthRegister =
                    getInstruction<OneRegisterInstruction>(videoLengthRegisterIndex).registerA
                val dummyRegisterForLong =
                    videoLengthRegister + 1 // required for long values since they are wide

                addInstruction(
                    videoLengthRegisterIndex + 1,
                    "invoke-static {v$videoLengthRegister, v$dummyRegisterForLong}, $INTEGRATIONS_CLASS_DESCRIPTOR->setVideoLength(J)V"
                )
            }
        }

        /**
         * Set current video id
         */
        VideoIdPatch.hookVideoId("$INTEGRATIONS_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")

        /**
         * Hook current playback speed
         */
        PlaybackSpeedFingerprint.resolve(
            context,
            PlaybackSpeedParentFingerprint.resultOrThrow().classDef
        )
        PlaybackSpeedFingerprint.resultOrThrow().let {
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

    private fun MutableMethod.insert(insertIndex: Int, register: String, descriptor: String) =
        addInstruction(insertIndex, "invoke-static { $register }, $descriptor")

    private fun MutableMethod.insertTimeHook(insertIndex: Int, descriptor: String) =
        insert(insertIndex, "p1, p2", descriptor)

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