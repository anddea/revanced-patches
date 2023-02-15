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
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.ads.doublebacktoclose.patch.DoubleBackToClosePatch
import app.revanced.patches.youtube.misc.litho.fingerprints.LithoFingerprint
import app.revanced.patches.youtube.ads.swiperefresh.patch.SwipeRefreshPatch
import app.revanced.util.integrations.Constants.ADS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

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
        LithoFingerprint
    )
)
{
    override fun execute(context: BytecodeContext): PatchResult {

        LithoFingerprint.result?.mutableMethod?.let {
            val implementations = it.implementation!!
            val instructions = implementations.instructions
            val parameter = it.parameters[2]
            val stringRegister = implementations.registerCount - it.parameters.size + 1
            val dummyRegister = stringRegister + 1

            val lithoIndex = instructions.indexOfFirst { instruction->
                instruction.opcode == Opcode.CONST_STRING &&
                        (instruction as BuilderInstruction21c).reference.toString() == "Element missing type extension"
            } + 2

            val firstReference = (instructions.elementAt(lithoIndex) as ReferenceInstruction).reference as MethodReference
            val secondReference = (instructions.elementAt(lithoIndex + 2) as ReferenceInstruction).reference as FieldReference

            it.addInstructions(
                0, """
                move-object/from16 v1, v$stringRegister
                invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;
                move-result-object v1
                move-object/from16 v2, v$dummyRegister
                iget-object v2, v2, $parameter->b:Ljava/nio/ByteBuffer;
                invoke-static {v1, v2}, $ADS_PATH/ExtendedLithoFilterPatch;->InflatedLithoView(Ljava/lang/String;Ljava/nio/ByteBuffer;)Z
                move-result v3
                if-eqz v3, :do_not_block
                move-object/from16 v0, p1
                invoke-static {v0}, $firstReference
                move-result-object v0
                iget-object v0, v0, ${secondReference.definingClass}->${secondReference.name}:${secondReference.type}
                return-object v0
            """, listOf(ExternalLabel("do_not_block", it.instruction(0)))
            )
        } ?: return LithoFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
