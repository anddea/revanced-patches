package app.revanced.patches.music.misc.litho.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.fingerprints.LithoFingerprint
import app.revanced.util.bytecode.getNarrowLiteralIndex
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicLithoFilterPatch : BytecodePatch(
    listOf(LithoFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        LithoFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetInstruction = implementation!!.instructions

                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val bufferIndex = getNarrowLiteralIndex(168777401)
                val bufferRegister = getInstruction<OneRegisterInstruction>(bufferIndex).registerA
                val targetIndex = targetInstruction.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.CONST_STRING &&
                            (instruction as BuilderInstruction21c).reference.toString() == "Element missing type extension"
                } + 2

                val builderMethodDescriptor = (getInstruction(targetIndex) as ReferenceInstruction).reference as MethodReference
                val emptyComponentFieldDescriptor = (getInstruction(targetIndex + 2) as ReferenceInstruction).reference as FieldReference
                val identifierRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

                targetInstruction.filter { instruction ->
                    val fieldReference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                    fieldReference?.let { reference -> reference.type == "Ljava/lang/StringBuilder;" } == true
                }.forEach { instruction ->
                    val insertIndex = targetInstruction.indexOf(instruction)
                    val stringBuilderRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                    addInstructionsWithLabels(
                        insertIndex, """
                            invoke-static {v$stringBuilderRegister, v$identifierRegister}, $MUSIC_ADS_PATH/MusicLithoFilterPatch;->filter(Ljava/lang/StringBuilder;Ljava/lang/String;)Z
                            move-result v$bufferRegister
                            if-eqz v$bufferRegister, :not_an_ad
                            move-object/from16 v$identifierRegister, p1
                            invoke-static {v$identifierRegister}, $builderMethodDescriptor
                            move-result-object v0
                            iget-object v0, v0, $emptyComponentFieldDescriptor
                            return-object v0
                            """, ExternalLabel("not_an_ad", getInstruction(insertIndex))
                    )
                }
            }
        } ?: return LithoFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
