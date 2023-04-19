package app.revanced.patches.youtube.misc.videoid.mainstream.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.*
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.VideoEndFingerprint
import app.revanced.patches.shared.fingerprints.VideoEndParentFingerprint
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.misc.timebar.patch.HookTimebarPatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.*
import app.revanced.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodParameter
import org.jf.dexlib2.util.MethodUtil

@Name("video-id-hook-mainstream")
@Description("Hook to detect when the video id changes (mainstream)")
@YouTubeCompatibility
@Version("0.0.1")
@DependsOn(
    [
        HookTimebarPatch::class,
        PlayerTypeHookPatch::class
    ]
)
class MainstreamVideoIdPatch : BytecodePatch(
    listOf(
        MainstreamVideoIdFingerprint,
        PlayerControllerSetTimeReferenceFingerprint,
        PlayerInitFingerprint,
        SeekFingerprint,
        TimebarFingerprint,
        VideoEndParentFingerprint,
        VideoTimeHighPrecisionFingerprint,
        VideoTimeHighPrecisionParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        VideoEndParentFingerprint.result?.classDef?.let { classDef ->
            VideoEndFingerprint.also {
                it.resolve(context, classDef)
            }.result?.mutableMethod?.let { method ->
                method.addInstruction(
                    method.implementation!!.instructions.size - 1,
                    "invoke-static {}, $VIDEO_PATH/VideoInformation;->videoEnd()V"
                )
            } ?: return VideoEndFingerprint.toErrorResult()
        } ?: return VideoEndParentFingerprint.toErrorResult()

        PlayerInitFingerprint.result?.let { parentResult ->
            playerInitMethod = parentResult.mutableClass.methods.first { MethodUtil.isConstructor(it) }

            // hook the player controller for use through integrations
            onCreateHook(INTEGRATIONS_CLASS_DESCRIPTOR, "playerController_onCreateHook")

            SeekFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                val resultMethod = it.method

                with (it.mutableMethod) {
                    val seekHelperMethod = ImmutableMethod(
                        resultMethod.definingClass,
                        "seekTo",
                        listOf(ImmutableMethodParameter("J", null, "time")),
                        "Z",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        null, null,
                        MutableMethodImplementation(4)
                    ).toMutable()

                    val seekSourceEnumType = resultMethod.parameterTypes[1].toString()

                    seekHelperMethod.addInstructions(
                        0,
                        """
                            sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                            invoke-virtual {p0, p1, p2, v0}, ${resultMethod.definingClass}->${resultMethod.name}(J$seekSourceEnumType)Z
                            move-result p1
                            return p1
                            """
                    )

                    parentResult.mutableClass.methods.add(seekHelperMethod)
                }
            } ?: return SeekFingerprint.toErrorResult()
        } ?: return PlayerInitFingerprint.toErrorResult()

        /*
         * Set the high precision video time method
         */
        VideoTimeHighPrecisionParentFingerprint.result?.let { parentResult ->
            VideoTimeHighPrecisionFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.let { method ->
                highPrecisionTimeMethod = method
            } ?: return VideoTimeHighPrecisionFingerprint.toErrorResult()
        } ?: return VideoTimeHighPrecisionParentFingerprint.toErrorResult()

        /*
         * Hook the methods which set the time
         */
        highPrecisionTimeHook(INTEGRATIONS_CLASS_DESCRIPTOR, "setVideoTime")

        /*
         * Set current video time
         */
        PlayerControllerSetTimeReferenceFingerprint.result?.let {
            timeMethod = context.toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                .getMethod() as MutableMethod
        } ?: return PlayerControllerSetTimeReferenceFingerprint.toErrorResult()


        with (HookTimebarPatch.emptyColorMethod) {
            val timeBarResult = TimebarFingerprint.result ?: return TimebarFingerprint.toErrorResult()
            val timeBarInstructions = timeBarResult.method.implementation!!.instructions
            val timeBarReference =
                (timeBarInstructions.elementAt(2) as ReferenceInstruction).reference as MethodReference

            val instructions = implementation!!.instructions

            reactReference =
                ((instructions.elementAt(instructions.count() - 3) as ReferenceInstruction).reference as FieldReference).name

            for ((index, instruction) in instructions.withIndex()) {
                val fieldReference = (instruction as? ReferenceInstruction)?.reference as? DexBackedMethodReference
                if (fieldReference?.let { it.name == timeBarReference.name } == true) {
                    val primaryRegister = (instructions.elementAt(index + 1) as OneRegisterInstruction).registerA
                    val secondaryRegister = primaryRegister + 1
                    addInstruction(
                        index + 3,
                        "invoke-static {v$primaryRegister, v$secondaryRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setVideoLength(J)V"
                    )
                    break
                }
            }
        }


        MainstreamVideoIdFingerprint.result?.let {
            insertIndex = it.scanResult.patternScanResult!!.endIndex

            with (it.mutableMethod) {
                insertMethod = this
                videoIdRegister = (implementation!!.instructions[insertIndex] as OneRegisterInstruction).registerA
            }
            offset++ // offset so setVideoId is called before any injected call
        } ?: return MainstreamVideoIdFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$VIDEO_PATH/VideoInformation;"
        internal var reactReference: String? = null

        private var offset = 0

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

        private fun MutableMethod.insert(insert: InsertIndex, register: String, descriptor: String) =
            addInstruction(insert.index, "invoke-static { $register }, $descriptor")

        private fun MutableMethod.insertTimeHook(insert: InsertIndex, descriptor: String) =
            insert(insert, "p1, p2", descriptor)

        /**
         * Hook the player controller.
         *
         * @param targetMethodClass The descriptor for the class to invoke when the player controller is created.
         * @param targetMethodName The name of the static method to invoke when the player controller is created.
         */
        internal fun onCreateHook(targetMethodClass: String, targetMethodName: String) =
            playerInitMethod.insert(
                InsertIndex.CREATE,
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
                InsertIndex.TIME,
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
                InsertIndex.HIGH_PRECISION_TIME,
                "$targetMethodClass->$targetMethodName(J)V"
            )

        enum class InsertIndex(internal val index: Int) {
            CREATE(4),
            TIME(2),
            HIGH_PRECISION_TIME(0),
        }
    }
}

