package app.revanced.patches.youtube.utils.videoid.general.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.videoid.general.fingerprint.PlayerControllerSetTimeReferenceFingerprint
import app.revanced.patches.youtube.utils.videoid.general.fingerprint.PlayerInitFingerprint
import app.revanced.patches.youtube.utils.videoid.general.fingerprint.SeekFingerprint
import app.revanced.patches.youtube.utils.videoid.general.fingerprint.VideoIdFingerprint
import app.revanced.patches.youtube.utils.videoid.general.fingerprint.VideoIdParentFingerprint
import app.revanced.patches.youtube.utils.videoid.general.fingerprint.VideoLengthFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

@DependsOn([PlayerTypeHookPatch::class])
class VideoIdPatch : BytecodePatch(
    listOf(
        PlayerControllerSetTimeReferenceFingerprint,
        PlayerInitFingerprint,
        SeekFingerprint,
        VideoIdParentFingerprint,
        VideoLengthFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

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
                        listOf(ImmutableMethodParameter("J", annotations, "time")),
                        "Z",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        annotations, null,
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
            } ?: throw SeekFingerprint.exception
        } ?: throw PlayerInitFingerprint.exception

        /**
         * Set current video time
         */
        PlayerControllerSetTimeReferenceFingerprint.result?.let {
            timeMethod = context.toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                .getMethod() as MutableMethod
        } ?: throw PlayerControllerSetTimeReferenceFingerprint.exception

        /**
         * Hook the methods which set the time
         */
        videoTimeHook(INTEGRATIONS_CLASS_DESCRIPTOR, "setVideoTime")

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
        } ?: throw VideoLengthFingerprint.exception

        VideoIdParentFingerprint.result?.let { parentResult ->
            VideoIdFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    insertMethod = this
                    insertIndex = it.scanResult.patternScanResult!!.endIndex
                    videoIdRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA
                }
                offset++ // offset so setVideoId is called before any injected call
            } ?: throw VideoIdFingerprint.exception
        } ?: throw VideoIdParentFingerprint.exception

        injectCall("$VIDEO_PATH/VideoInformation;->setVideoId(Ljava/lang/String;)V")

    }

    companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$VIDEO_PATH/VideoInformation;"

        private var offset = 0
        private var playerInitInsertIndex = 4
        private var timeInitInsertIndex = 2

        private var insertIndex: Int = 0
        private var videoIdRegister: Int = 0
        private lateinit var insertMethod: MutableMethod
        private lateinit var playerInitMethod: MutableMethod
        private lateinit var timeMethod: MutableMethod

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
    }
}

