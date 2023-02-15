package app.revanced.patches.youtube.ads.general.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.ads.general.bytecode.fingerprints.ComponentContextParserFingerprint
import app.revanced.patches.youtube.ads.general.bytecode.fingerprints.EmptyComponentBuilderFingerprint
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import app.revanced.util.integrations.Constants.ADS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@DependsOn([ResourceMappingPatch::class])
@Name("hide-general-ads-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class GeneralAdsBytecodePatch : BytecodePatch(
    listOf(ComponentContextParserFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ComponentContextParserFingerprint.result?.let { result ->
            val builderMethodIndex = EmptyComponentBuilderFingerprint
                .also { it.resolve(context, result.mutableMethod, result.mutableClass) }
                .let { it.result?.scanResult?.patternScanResult?.startIndex?: return EmptyComponentBuilderFingerprint.toErrorResult() }

            val emptyComponentFieldIndex = builderMethodIndex + 2

            with(result.mutableMethod) {
                val insertHookIndex = implementation!!.instructions.indexOfFirst {
                    it.opcode == Opcode.CONST_16 &&
                    (it as BuilderInstruction21s).narrowLiteral == 124
                } + 3

                val stringBuilderRegister = (instruction(insertHookIndex) as OneRegisterInstruction).registerA
                val clobberedRegister = (instruction(insertHookIndex - 3) as OneRegisterInstruction).registerA

                val bufferIndex = implementation!!.instructions.indexOfFirst {
                    it.opcode == Opcode.CONST &&
                    (it as Instruction31i).narrowLiteral == 183314536
                } - 1

                val bufferRegister = (instruction(bufferIndex) as OneRegisterInstruction).registerA

                val builderMethodDescriptor = instruction(builderMethodIndex).toDescriptor()
                val emptyComponentFieldDescriptor = instruction(emptyComponentFieldIndex).toDescriptor()

                addInstructions(
                    insertHookIndex, // right after setting the component.pathBuilder field,
                    """
                        invoke-static {v$stringBuilderRegister, v$bufferRegister}, $ADS_PATH/LithoFilterPatch;->filter(Ljava/lang/StringBuilder;Ljava/lang/String;)Z
                        move-result v$clobberedRegister
                        if-eqz v$clobberedRegister, :not_an_ad
                        move-object/from16 v$bufferRegister, p1
                        invoke-static {v$bufferRegister}, $builderMethodDescriptor
                        move-result-object v0
                        iget-object v0, v0, $emptyComponentFieldDescriptor
                        return-object v0
                    """,
                    listOf(ExternalLabel("not_an_ad", instruction(insertHookIndex)))
                )
            }
        } ?: return ComponentContextParserFingerprint.toErrorResult()

        context.updatePatchStatus("GeneralAds")

        return PatchResultSuccess()
    }

    private companion object {
        fun Instruction.toDescriptor() = when (val reference = (this as? ReferenceInstruction)?.reference) {
            is MethodReference -> "${reference.definingClass}->${reference.name}(${
                reference.parameterTypes.joinToString(
                    ""
                ) { it }
            })${reference.returnType}"
            is FieldReference -> "${reference.definingClass}->${reference.name}:${reference.type}"
            else -> throw PatchResultError("Unsupported reference type")
        }
    }
}
