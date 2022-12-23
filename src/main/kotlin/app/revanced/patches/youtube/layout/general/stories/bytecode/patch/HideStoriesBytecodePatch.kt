package app.revanced.patches.youtube.layout.general.stories.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.extensions.injectHideCall
import app.revanced.shared.patches.mapping.ResourceMappingPatch
import org.jf.dexlib2.iface.instruction.formats.Instruction22c
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.Opcode

@Name("hide-stories-bytecode-patch")
@DependsOn([ResourceMappingPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HideStoriesBytecodePatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "reel_multiple_items_shelf",
        "reel_item_container"
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
                                    resourceIds[0], resourceIds[1] -> { // reel ads
                                        val insertIndex = index + 4
                                        val iPutInstruction = instructions.elementAt(insertIndex)
                                        if (iPutInstruction.opcode != Opcode.IPUT_OBJECT) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (iPutInstruction as Instruction22c).registerA
                                        mutableMethod.implementation!!.injectHideCall(insertIndex, viewRegister, "layout/GeneralLayoutPatch", "hideStoriesShelf")
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
