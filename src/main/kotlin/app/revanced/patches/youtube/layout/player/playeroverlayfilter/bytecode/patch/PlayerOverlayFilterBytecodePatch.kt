package app.revanced.patches.youtube.layout.player.playeroverlayfilter.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.extensions.injectHideCall
import app.revanced.shared.patches.mapping.ResourceMappingPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.Opcode

@DependsOn([ResourceMappingPatch::class])
@Name("hide-player-overlay-filter-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class PlayerOverlayFilterBytecodePatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "scrim_overlay",
        "google_transparent"
    ).map { name ->
        ResourceMappingPatch.resourceMappings.single { it.name == name }.id
    }

    override fun execute(context: BytecodeContext): PatchResult {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                with(method.implementation) {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        when (instruction.opcode) {
                            Opcode.CONST -> {
                                when ((instruction as Instruction31i).wideLiteral) {
                                    resourceIds[0] -> { // player overlay filter
                                        val insertIndex = index + 3
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)
                                        val dummyRegister = (instructions.elementAt(index) as Instruction31i).registerA
                                        val viewRegister = (invokeInstruction as Instruction21c).registerA

                                        val transparent = resourceIds[1]

                                        mutableMethod.addInstructions(
                                            insertIndex + 1, """
                                                invoke-static {}, Lapp/revanced/integrations/patches/layout/PlayerLayoutPatch;->hidePlayerOverlayFilter()Z
                                                move-result v$dummyRegister
                                                if-eqz v$dummyRegister, :currentcolor
                                                const v$dummyRegister, $transparent
                                                invoke-virtual {v$viewRegister, v$dummyRegister}, Landroid/widget/ImageView;->setImageResource(I)V
                                            """, listOf(ExternalLabel("currentcolor", mutableMethod.instruction(insertIndex + 1)))
                                        )
                                    }
                                }
                            }
                            else -> return@forEachIndexed
                        }
                    }
                }
            }
        }
        return PatchResultSuccess()
    }
}
