package app.revanced.patches.music.misc.litho.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.fingerprints.LithoFingerprint
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicLithoFilterPatch : BytecodePatch(
    listOf(
        LithoFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        LithoFingerprint.result?.let { result ->
            val endIndex = result.scanResult.patternScanResult!!.endIndex
            val method = result.mutableMethod

            with (method.implementation!!.instructions) {
                val bufferIndex = indexOfFirst {
                    it.opcode == Opcode.CONST &&
                            (it as Instruction31i).narrowLiteral == 168777401
                }
                val bufferRegister = (method.instruction(bufferIndex) as Instruction31i).registerA

                val targetIndex = indexOfFirst {
                    it.opcode == Opcode.CONST_STRING &&
                            (it as BuilderInstruction21c).reference.toString() == "Element missing type extension"
                } + 2

                val builderMethodDescriptor = (elementAt(targetIndex) as ReferenceInstruction).reference as MethodReference
                val emptyComponentFieldDescriptor = (elementAt(targetIndex + 2) as ReferenceInstruction).reference as FieldReference

                val identifierRegister = (method.instruction(endIndex) as OneRegisterInstruction).registerA

                filter { instruction ->
                    val fieldReference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                    fieldReference?.let { it.type == "Ljava/lang/StringBuilder;" } == true
                }.forEach { instruction ->
                    val insertIndex = indexOf(instruction)
                    val stringBuilderRegister = (method.instruction(insertIndex) as TwoRegisterInstruction).registerA

                    method.addInstructions(
                        insertIndex, // right after setting the component.pathBuilder field,
                        """
                        invoke-static {v$stringBuilderRegister, v$identifierRegister}, $MUSIC_ADS_PATH/MusicLithoFilterPatch;->filter(Ljava/lang/StringBuilder;Ljava/lang/String;)Z
                        move-result v$bufferRegister
                        if-eqz v$bufferRegister, :not_an_ad
                        move-object/from16 v$identifierRegister, p1
                        invoke-static {v$identifierRegister}, $builderMethodDescriptor
                        move-result-object v0
                        iget-object v0, v0, $emptyComponentFieldDescriptor
                        return-object v0
                    """, listOf(ExternalLabel("not_an_ad", method.instruction(insertIndex)))
                    )
                }
            }
        } ?: return LithoFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
