package app.revanced.patches.shared.patch.litho

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.fingerprints.LithoFingerprint
import app.revanced.patches.shared.fingerprints.LithoObjectFingerprint
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@Version("0.0.1")
abstract class AbstractLithoFilterPatch(
    private val descriptor: String
) : BytecodePatch(
    listOf(
        LithoFingerprint,
        LithoObjectFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
         * Compatible with both YouTube and YouTube Music
         * But not yet implemented in Music integrations
         */
        LithoObjectFingerprint.result?.let {
            val endIndex = it.scanResult.patternScanResult!!.endIndex
            objectRegister = (it.mutableMethod.instruction(endIndex) as BuilderInstruction35c).registerC
        } ?: return LithoObjectFingerprint.toErrorResult()

        LithoFingerprint.result?.let { result ->
            val endIndex = result.scanResult.patternScanResult!!.endIndex
            val method = result.mutableMethod

            with (method.implementation!!.instructions) {
                val targetIndex = indexOfFirst {
                    it.opcode == Opcode.CONST_STRING &&
                            (it as BuilderInstruction21c).reference.toString() == "Element missing type extension"
                } + 2

                val builderMethodDescriptor = (elementAt(targetIndex) as ReferenceInstruction).reference as MethodReference
                val emptyComponentFieldDescriptor = (elementAt(targetIndex + 2) as ReferenceInstruction).reference as FieldReference

                val identifierRegister = (method.instruction(endIndex) as OneRegisterInstruction).registerA
                val bytebufferRegister = method.implementation!!.registerCount - method.parameters.size + 2
                val secondParameter = method.parameters[2]

                filter { instruction ->
                    val fieldReference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                    fieldReference?.let { it.type == "Ljava/lang/StringBuilder;" } == true
                }.forEach { instruction ->
                    val insertIndex = indexOf(instruction) + 1

                    val stringBuilderRegister = (method.instruction(insertIndex) as OneRegisterInstruction).registerA
                    val clobberedRegister = (method.instruction(insertIndex + 1) as TwoRegisterInstruction).registerA

                    method.addInstructions(
                        insertIndex, // right after setting the component.pathBuilder field,
                        """
                        move-object/from16 v$clobberedRegister, v$bytebufferRegister
                        iget-object v$clobberedRegister, v$clobberedRegister, $secondParameter->b:Ljava/nio/ByteBuffer;
                        invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister, v$clobberedRegister}, $descriptor->filter(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;Ljava/nio/ByteBuffer;)Z
                        move-result v$clobberedRegister
                        if-eqz v$clobberedRegister, :not_an_ad
                        move-object/from16 v$identifierRegister, p1
                        invoke-static {v$identifierRegister}, $builderMethodDescriptor
                        move-result-object v0
                        iget-object v0, v0, $emptyComponentFieldDescriptor
                        return-object v0
                    """,listOf(ExternalLabel("not_an_ad", method.instruction(insertIndex)))
                    )
                }
            }
        } ?: return LithoFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
    private companion object {
        private var objectRegister: Int = 3
    }
}
