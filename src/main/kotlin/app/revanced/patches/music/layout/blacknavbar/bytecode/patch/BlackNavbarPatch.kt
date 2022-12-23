package app.revanced.patches.music.layout.blacknavbar.bytecode.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.patches.mapping.ResourceMappingPatch
import org.jf.dexlib2.iface.instruction.formats.Instruction11x
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.Opcode

@Patch
@DependsOn(
    [
        MusicIntegrationsPatch::class,
        MusicSettingsPatch::class,
        ResourceMappingPatch::class
    ]
)
@Name("black-navbar")
@Description("Sets the navigation bar color to black.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class BlackNavbarPatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "ytm_color_grey_12"
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
                                    resourceIds[0] -> { // blacknavbar
                                        val insertIndex = index - 1
                                        val insertIndex2 = index + 2
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        val invokeInstruction2 = instructions.elementAt(insertIndex2)
                                        if (invokeInstruction.opcode != Opcode.MOVE_RESULT_OBJECT) return@forEachIndexed

                                        val register1 = (instructions.elementAt(index) as Instruction31i).registerA
                                        val register2 = (invokeInstruction2 as Instruction11x).registerA

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        mutableMethod.addInstructions(
                                            index + 3, """
                                                invoke-static {}, Lapp/revanced/integrations/settings/MusicSettings;->getBlackNavbar()Z
                                                move-result v$register1
                                                if-eqz v$register1, :default
                                                const/high16 v$register2, -0x1000000
                                                """, listOf(ExternalLabel("default", mutableMethod.instruction(index + 3)))
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
