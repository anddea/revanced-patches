package app.revanced.patches.youtube.misc.litho.patch

import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.util.integrations.Constants.ADS_PATH

@DependsOn(
    [
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ByteBufferFilterPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        return PatchResultSuccess()
    }

    companion object{
        fun inject(descriptor: String){
            LithoFilterPatch.lithoMethod.addInstructions(
                0, """
                    move-object/from16 v10, p3
                    iget-object v10, v10, ${LithoFilterPatch.objectReference}
                    if-eqz v10, :do_not_block
                    check-cast v10, ${LithoFilterPatch.bufferReference}
                    iget-object v10, v10, ${LithoFilterPatch.bufferReference}->b:Ljava/nio/ByteBuffer;
                    move-object/from16 v3, p2
                    invoke-static {v3, v10}, $descriptor(Ljava/lang/Object;Ljava/nio/ByteBuffer;)Z
                    move-result v10
                    if-eqz v10, :do_not_block
                    move-object/from16 v15, p1
                    invoke-static {v15}, ${LithoFilterPatch.builderMethodDescriptor}
                    move-result-object v0
                    iget-object v0, v0, ${LithoFilterPatch.emptyComponentFieldDescriptor}
                    return-object v0
                    """, listOf(ExternalLabel("do_not_block", LithoFilterPatch.lithoMethod.instruction(0)))
            )
        }
    }
}
