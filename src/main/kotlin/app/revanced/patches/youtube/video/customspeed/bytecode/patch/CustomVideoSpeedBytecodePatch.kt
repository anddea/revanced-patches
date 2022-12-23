package app.revanced.patches.youtube.video.customspeed.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.video.customspeed.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.patches.options.PatchOptions
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.builder.instruction.BuilderArrayPayload
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@Name("custom-speed-bytecode-patch")
@DependsOn([PatchOptions::class])
@YouTubeCompatibility
@Version("0.0.1")
class CustomVideoSpeedBytecodePatch : BytecodePatch(
    listOf(
        SpeedArrayGeneratorFingerprint,
        SpeedLimiterFingerprint,
        VideoSpeedEntriesFingerprint
    )
) {
    private companion object {
        const val INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoSpeedPatch;"

        const val INTEGRATIONS_VIDEO_SPEED_ENTRIES_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoSpeedEntries;"
    }
    override fun execute(context: BytecodeContext): PatchResult {
        var speed = PatchOptions.CustomSpeedArrays
        val splits = speed!!.replace(" ","").split(",")
        if (splits.count() < 1) throw IllegalArgumentException("Invalid speed elements")
        val videoSpeedsArray = splits.map { it.toFloat().toRawBits() }

        val arrayGenMethod = SpeedArrayGeneratorFingerprint.result?.mutableMethod!!
        val arrayGenMethodImpl = arrayGenMethod.implementation!!

        val sizeCallIndex = arrayGenMethodImpl.instructions
            .indexOfFirst { ((it as? ReferenceInstruction)?.reference as? MethodReference)?.name == "size" }

        if (sizeCallIndex == -1) return PatchResultError("Couldn't find call to size()")

        val sizeCallResultRegister =
            (arrayGenMethodImpl.instructions.elementAt(sizeCallIndex + 1) as OneRegisterInstruction).registerA

        arrayGenMethod.addInstructions(
            sizeCallIndex + 2,
            """
            invoke-static {}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->isCustomVideoSpeedEnabled()Z
            move-result v9
            if-eqz v9, :defaultspeed
            const/4 v$sizeCallResultRegister, 0x0
            """, listOf(ExternalLabel("defaultspeed", arrayGenMethod.instruction(sizeCallIndex + 2)))
        )


        val (arrayLengthConstIndex, arrayLengthConst) = arrayGenMethodImpl.instructions.withIndex()
            .first { (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 7 }

        val arrayLengthConstDestination = (arrayLengthConst as OneRegisterInstruction).registerA

        val videoSpeedsArrayType = "$INTEGRATIONS_VIDEO_SPEED_ENTRIES_CLASS_DESCRIPTOR->videoSpeed:[F"

        arrayGenMethod.addInstructions(
            arrayLengthConstIndex + 1,
            """
            if-eqz v9, :defaultspeed
            sget-object v$arrayLengthConstDestination, $videoSpeedsArrayType
            array-length v$arrayLengthConstDestination, v$arrayLengthConstDestination
            """, listOf(ExternalLabel("defaultspeed", arrayGenMethod.instruction(arrayLengthConstIndex + 1)))
        )

        val (originalArrayFetchIndex, originalArrayFetch) = arrayGenMethodImpl.instructions.withIndex()
            .first {
                val reference = ((it.value as? ReferenceInstruction)?.reference as? FieldReference)
                reference?.definingClass?.contains("PlayerConfigModel") ?: false &&
                        reference?.type == "[F"
            }

        val originalArrayFetchDestination = (originalArrayFetch as OneRegisterInstruction).registerA

        arrayGenMethod.addInstructions(
            originalArrayFetchIndex + 1,
            """
            if-eqz v9, :defaultspeed
            sget-object v$originalArrayFetchDestination, $videoSpeedsArrayType
            """, listOf(ExternalLabel("defaultspeed", arrayGenMethod.instruction(originalArrayFetchIndex + 1)))
        )

        val limiterMethod = SpeedLimiterFingerprint.result?.mutableMethod!!;
        val limiterMethodImpl = limiterMethod.implementation!!

        val speedLimitMin = 0.0f
        val speedLimitMax = 100f

        val (limiterMinConstIndex, limiterMinConst) = limiterMethodImpl.instructions.withIndex()
            .first { (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 0.25f.toRawBits() }
        val (limiterMaxConstIndex, limiterMaxConst) = limiterMethodImpl.instructions.withIndex()
            .first { (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits() }

        val limiterMinConstDestination = (limiterMinConst as OneRegisterInstruction).registerA
        val limiterMaxConstDestination = (limiterMaxConst as OneRegisterInstruction).registerA

        fun hexFloat(float: Float): String = "0x%08x".format(float.toRawBits())

        limiterMethod.replaceInstruction(
            limiterMinConstIndex,
            "const/high16 v$limiterMinConstDestination, ${hexFloat(speedLimitMin)}"
        )
        limiterMethod.replaceInstruction(
            limiterMaxConstIndex,
            "const/high16 v$limiterMaxConstDestination, ${hexFloat(speedLimitMax)}"
        )

        val constructorResult = VideoSpeedEntriesFingerprint.result!!
        val constructor = constructorResult.mutableMethod
        val implementation = constructor.implementation!!

        constructor.replaceInstruction(
            0,
            "const/16 v0, ${videoSpeedsArray.size}"
        )

        val arrayPayloadIndex = implementation.instructions.size - 1
        implementation.replaceInstruction(
            arrayPayloadIndex,
            BuilderArrayPayload(
                4,
                videoSpeedsArray
            )
        )

        return PatchResultSuccess()
    }
}
