package app.revanced.patches.youtube.layout.general.headerswitch.bytecode.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.toResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@DependsOn([ResourceMappingPatch::class])
@Name("header-switch-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HeaderSwitchBytecodePatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "ytWordmarkHeader"
    ).map { name ->
        ResourceMappingPatch.resourceMappings.single { it.name == name }.id
    }
    private var patchSuccessArray = Array(resourceIds.size) {false}

    override fun execute(context: BytecodeContext): PatchResult {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                with(method.implementation) {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        when (instruction.opcode) {
                            Opcode.CONST -> {
                                when ((instruction as Instruction31i).wideLiteral) {
                                    resourceIds[0] -> { // header
                                        val insertIndex = index + 1

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (instructions.elementAt(index) as Instruction31i).registerA

                                        mutableMethod.addInstructions(
                                            insertIndex, """
                                                invoke-static {v$viewRegister}, Lapp/revanced/integrations/patches/layout/GeneralLayoutPatch;->enablePremiumHeader(I)I
                                                move-result v$viewRegister
                                            """
                                        )

                                        patchSuccessArray[0] = true;
                                    }
                                }
                            }
                            else -> return@forEachIndexed
                        }
                    }
                }
            }
        }
        return toResult(patchSuccessArray.indexOf(false))
    }
}
