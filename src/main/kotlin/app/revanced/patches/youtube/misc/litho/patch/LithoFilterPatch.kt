package app.revanced.patches.youtube.misc.litho.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.LithoBufferFingerprint
import app.revanced.patches.shared.fingerprints.LithoFingerprint
import app.revanced.patches.shared.fingerprints.LithoObjectFingerprint
import app.revanced.patches.youtube.ads.doublebacktoclose.patch.DoubleBackToClosePatch
import app.revanced.patches.youtube.ads.swiperefresh.patch.SwipeRefreshPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import app.revanced.util.bytecode.getNarrowLiteralIndex
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.integrations.Constants.ADS_PATH
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.Reference
import kotlin.properties.Delegates

@DependsOn(
    [
        DoubleBackToClosePatch::class,
        SwipeRefreshPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class LithoFilterPatch : BytecodePatch(
    listOf(
        LithoFingerprint,
        LithoBufferFingerprint,
        LithoObjectFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        LithoBufferFingerprint.result?.let {
            val startIndex = it.scanResult.patternScanResult!!.startIndex
            bufferReference = it.mutableMethod.getInstruction<ReferenceInstruction>(startIndex).reference
        } ?: return LithoBufferFingerprint.toErrorResult()

        LithoObjectFingerprint.result?.let {
            val endIndex = it.scanResult.patternScanResult!!.endIndex
            objectRegister = it.mutableMethod.getInstruction<BuilderInstruction35c>(endIndex).registerC
        } ?: return LithoObjectFingerprint.toErrorResult()

        LithoFingerprint.result?.let { result ->
            val endIndex = result.scanResult.patternScanResult!!.endIndex
            lithoMethod = result.mutableMethod

            lithoMethod.apply {
                val bufferIndex = getNarrowLiteralIndex(168777401)
                val bufferRegister = getInstruction<OneRegisterInstruction>(bufferIndex).registerA
                val targetIndex = getStringIndex("Element missing type extension") + 2
                val identifierRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

                builderMethodDescriptor = getInstruction<ReferenceInstruction>(targetIndex).reference
                emptyComponentFieldDescriptor = getInstruction<ReferenceInstruction>(targetIndex + 2).reference
                implementation!!.instructions.apply {
                    filter { instruction ->
                        val fieldReference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        fieldReference?.let { it.type == "Ljava/lang/StringBuilder;" } == true
                    }.forEach { instruction ->
                        val insertIndex = indexOf(instruction)
                        val stringBuilderRegister = lithoMethod.getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                        val objectIndex = lithoMethod.getStringIndex("") - 2
                        objectReference = lithoMethod.getInstruction<ReferenceInstruction>(objectIndex).reference
                        lithoMethod.addInstructionsWithLabels(
                            insertIndex + 1, """
                                move-object/from16 v$bufferRegister, p3
                                iget-object v$bufferRegister, v$bufferRegister, $objectReference
                                if-eqz v$bufferRegister, :not_an_ad
                                check-cast v$bufferRegister, $bufferReference
                                iget-object v$bufferRegister, v$bufferRegister, $bufferReference->b:Ljava/nio/ByteBuffer;
                                invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister, v$bufferRegister}, $ADS_PATH/LithoFilterPatch;->filters(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;Ljava/nio/ByteBuffer;)Z
                                move-result v$bufferRegister
                                if-eqz v$bufferRegister, :not_an_ad
                                move-object/from16 v$identifierRegister, p1
                                invoke-static {v$identifierRegister}, $builderMethodDescriptor
                                move-result-object v0
                                iget-object v0, v0, $emptyComponentFieldDescriptor
                                return-object v0
                                """, ExternalLabel("not_an_ad", lithoMethod.getInstruction(insertIndex + 1))
                        )
                    }
                }
            }
        } ?: return LithoFingerprint.toErrorResult()

        context.updatePatchStatus("ByteBuffer")

        return PatchResultSuccess()
    }
    internal companion object {
        var objectRegister by Delegates.notNull<Int>()

        lateinit var lithoMethod: MutableMethod
        lateinit var bufferReference: Reference
        lateinit var builderMethodDescriptor: Reference
        lateinit var emptyComponentFieldDescriptor: Reference
        lateinit var objectReference: Reference
    }
}
