package app.revanced.patches.youtube.utils.videoid.mainstream.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint.MainstreamVideoIdFingerprint
import app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint.PlayerControllerSetTimeReferenceFingerprint
import app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint.PlayerInitFingerprint
import app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint.SeekFingerprint
import app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint.TimebarFingerprint
import app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint.VideoLengthFingerprint
import app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint.VideoTimeHighPrecisionFingerprint
import app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint.VideoTimeHighPrecisionParentFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodParameter
import org.jf.dexlib2.util.MethodUtil

@Name("video-id-hook-mainstream")
@Description("Hook to detect when the video id changes (mainstream)")
@YouTubeCompatibility
@Version("0.0.1")
@DependsOn([PlayerTypeHookPatch::class])
class MainstreamVideoIdPatch : BytecodePatch(
    listOf(
        MainstreamVideoIdFingerprint,
        PlayerControllerSetTimeReferenceFingerprint,
        PlayerInitFingerprint,
        SeekFingerprint,
        TimebarFingerprint,
        VideoLengthFingerprint,
        VideoTimeHighPrecisionFingerprint,
        VideoTimeHighPrecisionParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PlayerInitFingerprint.result?.let { parentResult ->
            playerInitMethod =
                parentResult.mutableClass.methods.first { MethodUtil.isConstructor(it) }

            // hook the player controller for use through integrations
            onCreateHook(INTEGRATIONS_CLASS_DESCRIPTOR, "initialize")

            SeekFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val seekHelperMethod = ImmutableMethod(
                        definingClass,
                        "seekTo",
                        listOf(ImmutableMethodParameter("J", null, "time")),
                        "Z",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        null, null,
                        MutableMethodImplementation(4)
                    ).toMutable()

                    val seekSourceEnumType = parameterTypes[1].toString()

                    seekHelperMethod.addInstructions(
                        0, """
                            sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                            invoke-virtual {p0, p1, p2, v0}, ${definingClass}->${name}(J$seekSourceEnumType)Z
                            move-result p1
                            return p1
                            """
                    )

                    parentResult.mutableClass.methods.add(seekHelperMethod)
                }
            } ?: return SeekFingerprint.toErrorResult()
        } ?: return PlayerInitFingerprint.toErrorResult()

        /**
         * Set the high precision video time method
         */
        VideoTimeHighPrecisionParentFingerprint.result?.let { parentResult ->
            VideoTimeHighPrecisionFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.mutableMethod?.let { method ->
                highPrecisionTimeMethod = method
            } ?: return VideoTimeHighPrecisionFingerprint.toErrorResult()
        } ?: return VideoTimeHighPrecisionParentFingerprint.toErrorResult()

        /**
         * Hook the methods which set the time
         */
        highPrecisionTimeHook(INTEGRATIONS_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Set current video time
         */
        PlayerControllerSetTimeReferenceFingerprint.result?.let {
            timeMethod = context.toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                .getMethod() as MutableMethod
        } ?: return PlayerControllerSetTimeReferenceFingerprint.toErrorResult()

        /**
         * Set current video length
         */
        VideoLengthFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val primaryRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA
                val secondaryRegister = primaryRegister + 1

                addInstruction(
                    startIndex + 2,
                    "invoke-static {v$primaryRegister, v$secondaryRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setVideoLength(J)V"
                )
            }
        } ?: return VideoLengthFingerprint.toErrorResult()

        MainstreamVideoIdFingerprint.result?.let {
            it.mutableMethod.apply {
                insertMethod = this
                insertIndex = it.scanResult.patternScanResult!!.endIndex
                videoIdRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA
            }
            offset++ // offset so setVideoId is called before any injected call
        } ?: return MainstreamVideoIdFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$VIDEO_PATH/VideoInformation;"

        private var offset = 0
        private var playerInitInsertIndex = 4
        private var timeInitInsertIndex = 2
        private var highPrecisionInsertIndex = 0

        private var insertIndex: Int = 0
        private var videoIdRegister: Int = 0
        private lateinit var insertMethod: MutableMethod
        private lateinit var playerInitMethod: MutableMethod
        private lateinit var timeMethod: MutableMethod
        private lateinit var highPrecisionTimeMethod: MutableMethod

        /**
         * Adds an invoke-static instruction, called with the new id when the video changes
         * @param methodDescriptor which method to call. Params have to be `Ljava/lang/String;`
         */
        fun injectCall(
            methodDescriptor: String
        ) {
            insertMethod.addInstructions(
                insertIndex + offset, // move-result-object offset
                "invoke-static {v$videoIdRegister}, $methodDescriptor"
            )
        }

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
            playerInitMethod.insert(
                playerInitInsertIndex++,
                "v0",
                "$targetMethodClass->$targetMethodName(Ljava/lang/Object;)V"
            )

        /**
         * Hook the video time.
         * The hook is usually called once per second.
         *
         * @param targetMethodClass The descriptor for the static method to invoke when the player controller is created.
         * @param targetMethodName The name of the static method to invoke when the player controller is created.
         */
        internal fun videoTimeHook(targetMethodClass: String, targetMethodName: String) =
            timeMethod.insertTimeHook(
                timeInitInsertIndex++,
                "$targetMethodClass->$targetMethodName(J)V"
            )

        /**
         * Hook the high precision video time.
         * The hooks is called extremely often (10 to 15 times a seconds), so use with caution.
         * Note: the hook is usually called _off_ the main thread
         *
         * @param targetMethodClass The descriptor for the static method to invoke when the player controller is created.
         * @param targetMethodName The name of the static method to invoke when the player controller is created.
         */
        internal fun highPrecisionTimeHook(targetMethodClass: String, targetMethodName: String) =
            highPrecisionTimeMethod.insertTimeHook(
                highPrecisionInsertIndex++,
                "$targetMethodClass->$targetMethodName(J)V"
            )
    }
}

