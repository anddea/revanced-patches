package app.revanced.patches.youtube.misc.litho.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
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
import app.revanced.util.integrations.Constants.ADS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference
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

        try {
            with (LithoBufferFingerprint.result!!) {
                val startIndex = this.scanResult.patternScanResult!!.startIndex
                bufferReference = (this.mutableMethod.instruction(startIndex) as BuilderInstruction21c).reference.toString()
            }
            bufferFingerprintResolved = true
        } catch (_: Exception) {
            bufferFingerprintResolved = false
        }

        LithoObjectFingerprint.result?.let {
            val endIndex = it.scanResult.patternScanResult!!.endIndex
            objectRegister = (it.mutableMethod.instruction(endIndex) as BuilderInstruction35c).registerC
        } ?: return LithoObjectFingerprint.toErrorResult()

        LithoFingerprint.result?.let { result ->
            val endIndex = result.scanResult.patternScanResult!!.endIndex
            lithoMethod = result.mutableMethod

            with (lithoMethod.implementation!!.instructions) {
                // 18.06.41+
                val bufferIndex = indexOfFirst {
                    it.opcode == Opcode.CONST &&
                            (it as Instruction31i).narrowLiteral == 168777401
                }
                val bufferRegister = (lithoMethod.instruction(bufferIndex) as Instruction31i).registerA

                // 18.06.41+
                val targetIndex = indexOfFirst {
                    it.opcode == Opcode.CONST_STRING &&
                            (it as BuilderInstruction21c).reference.toString() == "Element missing type extension"
                } + 2
                builderMethodDescriptor = (elementAt(targetIndex) as ReferenceInstruction).reference as MethodReference
                emptyComponentFieldDescriptor = (elementAt(targetIndex + 2) as ReferenceInstruction).reference as FieldReference

                val identifierRegister = (lithoMethod.instruction(endIndex) as OneRegisterInstruction).registerA

                filter { instruction ->
                    val fieldReference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                    fieldReference?.let { it.type == "Ljava/lang/StringBuilder;" } == true
                }.forEach { instruction ->
                    val insertIndex = indexOf(instruction)
                    val stringBuilderRegister = (lithoMethod.instruction(insertIndex) as TwoRegisterInstruction).registerA

                    val instructionList =
                        """
                            invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister, v$bufferRegister}, $ADS_PATH/LithoFilterPatch;->filter(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;Ljava/nio/ByteBuffer;)Z
                            move-result v$bufferRegister
                            if-eqz v$bufferRegister, :not_an_ad
                            move-object/from16 v$identifierRegister, p1
                            invoke-static {v$identifierRegister}, $builderMethodDescriptor
                            move-result-object v0
                            iget-object v0, v0, $emptyComponentFieldDescriptor
                            return-object v0
                        """

                    if (bufferFingerprintResolved) {
                        // 18.11.35+
                        val objectIndex = indexOfFirst {
                            it.opcode == Opcode.CONST_STRING &&
                                    (it as BuilderInstruction21c).reference.toString() == ""
                        } - 2
                        objectReference = (elementAt(objectIndex) as ReferenceInstruction).reference as FieldReference
                        lithoMethod.addInstructions(
                            insertIndex + 1,
                            """
                                move-object/from16 v$bufferRegister, p3
                                iget-object v$bufferRegister, v$bufferRegister, ${objectReference.definingClass}->${objectReference.name}:${objectReference.type}
                                if-eqz v$bufferRegister, :not_an_ad
                                check-cast v$bufferRegister, $bufferReference
                                iget-object v$bufferRegister, v$bufferRegister, $bufferReference->b:Ljava/nio/ByteBuffer;
                            """ + instructionList,listOf(ExternalLabel("not_an_ad", lithoMethod.instruction(insertIndex + 1)))
                        )
                    } else {
                        val secondParameter = lithoMethod.parameters[2]
                        lithoMethod.addInstructions(
                            insertIndex + 1,
                            """
                                move-object/from16 v$bufferRegister, p3
                                iget-object v$bufferRegister, v$bufferRegister, $secondParameter->b:Ljava/nio/ByteBuffer;
                            """ + instructionList,listOf(ExternalLabel("not_an_ad", lithoMethod.instruction(insertIndex + 1)))
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
        var bufferFingerprintResolved by Delegates.notNull<Boolean>()

        lateinit var lithoMethod: MutableMethod
        lateinit var bufferReference: String
        lateinit var builderMethodDescriptor: MethodReference
        lateinit var emptyComponentFieldDescriptor: FieldReference
        lateinit var objectReference: FieldReference
    }
}
