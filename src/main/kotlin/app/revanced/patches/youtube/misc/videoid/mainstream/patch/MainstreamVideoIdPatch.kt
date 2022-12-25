package app.revanced.patches.youtube.misc.videoid.mainstream.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.*
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.patches.timebar.HookTimebarPatch
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
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
        PlayerControllerFingerprint,
        PlayerControllerSetTimeReferenceFingerprint,
        PlayerInitFingerprint,
        RepeatListenerFingerprint,
        SeekFingerprint,
        VideoTimeFingerprint,
        VideoTimeParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val VideoInformation = "$VIDEO_PATH/VideoInformation;"

        val RepeatListenerResult = RepeatListenerFingerprint.result!!
        val RepeatListenerMethod = RepeatListenerResult.mutableMethod
        val removeIndex = RepeatListenerResult.scanResult.patternScanResult!!.startIndex

        // RepeatListenerMethod.removeInstruction(removeIndex)
        RepeatListenerMethod.removeInstruction(removeIndex - 1)

        with(PlayerInitFingerprint.result!!) {
            PlayerInitMethod = mutableClass.methods.first { MethodUtil.isConstructor(it) }

            // seek method
            val seekFingerprintResultMethod = SeekFingerprint.also { it.resolve(context, classDef) }.result!!.method

            // create helper method
            val seekHelperMethod = ImmutableMethod(
                seekFingerprintResultMethod.definingClass,
                "seekTo",
                listOf(ImmutableMethodParameter("J", null, "time")),
                "Z",
                AccessFlags.PUBLIC or AccessFlags.FINAL,
                null, null,
                MutableMethodImplementation(4)
            ).toMutable()

            // get enum type for the seek helper method
            val seekSourceEnumType = seekFingerprintResultMethod.parameterTypes[1].toString()

            // insert helper method instructions
            seekHelperMethod.addInstructions(
                0,
                """
                sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                invoke-virtual {p0, p1, p2, v0}, ${seekFingerprintResultMethod.definingClass}->${seekFingerprintResultMethod.name}(J$seekSourceEnumType)Z
                move-result p1
                return p1
            """
            )

            // add the seekTo method to the class for the integrations to call
            mutableClass.methods.add(seekHelperMethod)
        }

        val VideoTimeParentResult = VideoTimeParentFingerprint.result!!
        VideoTimeFingerprint.resolve(context, VideoTimeParentResult.classDef)
        val VideoTimeMethod = VideoTimeFingerprint.result!!.mutableMethod
        VideoTimeMethod.addInstruction(
            0,
            "invoke-static {p1, p2}, $VideoInformation->setCurrentVideoTimeHighPrecision(J)V"
        )

        /*
        Set current video time
        */
        val referenceResult = PlayerControllerSetTimeReferenceFingerprint.result!!
        val PlayerControllerSetTimeMethod =
            context.toMethodWalker(referenceResult.method)
                .nextMethod(referenceResult.scanResult.patternScanResult!!.startIndex, true)
                .getMethod() as MutableMethod
        PlayerControllerSetTimeMethod.addInstruction(
            2,
            "invoke-static {p1, p2}, $VideoInformation->setCurrentVideoTime(J)V"
        )

        val EmptyColorMethod = HookTimebarPatch.EmptyColorFingerprintResult.mutableMethod
        val EmptyColorMethodInstructions = EmptyColorMethod.implementation!!.instructions

        val methodReference =
            HookTimebarPatch.TimbarFingerprintResult.method.let { method ->
                (method.implementation!!.instructions.elementAt(2) as ReferenceInstruction).reference as MethodReference
            }

        for ((index, instruction) in EmptyColorMethodInstructions.withIndex()) {
            if (instruction.opcode != Opcode.CHECK_CAST) continue
            val primaryRegister = (instruction as Instruction21c).registerA + 1
            val secondaryRegister = primaryRegister + 1
            EmptyColorMethod.addInstructions(
                index, """
                invoke-virtual {p0}, $methodReference
                move-result-wide v$primaryRegister
                invoke-static {v$primaryRegister, v$secondaryRegister}, $VideoInformation->setCurrentVideoLength(J)V
            """
            )
            break
        }

        val reactReference =
            ((EmptyColorMethodInstructions.elementAt(EmptyColorMethodInstructions.count() - 3) as ReferenceInstruction).reference as FieldReference).name

        val PlayerContrallerResult = PlayerControllerFingerprint.result!!
        val PlayerContrallerMethod = PlayerContrallerResult.mutableMethod
        val PlayerContrallerInstructions = PlayerContrallerMethod.implementation!!.instructions

        /*
         Get the instance of the seekbar rectangle
         */
        for ((index, instruction) in PlayerContrallerInstructions.withIndex()) {
            if (instruction.opcode != Opcode.CONST_STRING) continue
            val register = (instruction as OneRegisterInstruction).registerA
            PlayerContrallerMethod.replaceInstruction(
                index,
                "const-string v$register, \"$reactReference\""
            )
            break
        }


        InsertResult = MainstreamVideoIdFingerprint.result!!
        InsertMethod = InsertResult.mutableMethod
        InsertIndex = InsertResult.scanResult.patternScanResult!!.endIndex

        videoIdRegister =
            (InsertMethod.implementation!!.instructions[InsertIndex] as OneRegisterInstruction).registerA

        injectCall("$VideoInformation->setCurrentVideoId(Ljava/lang/String;)V")
        injectCallonCreate(VideoInformation, "onCreate")

        offset++ // offset so setCurrentVideoId is called before any injected call

        return PatchResultSuccess()
    }

    companion object {
        private var offset = 1

        private var videoIdRegister: Int = 0
        private var InsertIndex: Int = 0
        private lateinit var InsertResult: MethodFingerprintResult
        private lateinit var InsertMethod: MutableMethod
        private lateinit var PlayerInitMethod: MutableMethod

        /**
         * Adds an invoke-static instruction, called with the new id when the video changes
         * @param methodDescriptor which method to call. Params have to be `Ljava/lang/String;`
         */
        fun injectCall(
            methodDescriptor: String
        ) {
            InsertMethod.addInstructions(
                InsertIndex + offset, // move-result-object offset
                "invoke-static {v$videoIdRegister}, $methodDescriptor"
            )
        }

        fun injectCallonCreate(MethodClass: String, MethodName: String) =
            PlayerInitMethod.addInstruction(
                4,
                "invoke-static {v0}, $MethodClass->$MethodName(Ljava/lang/Object;)V"
            )
    }
}

