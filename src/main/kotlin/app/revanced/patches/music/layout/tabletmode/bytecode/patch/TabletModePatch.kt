package app.revanced.patches.music.layout.tabletmode.bytecode.patch

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
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.patches.mapping.ResourceMappingPatch
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
@Name("tablet-mode")
@Description("Unlocks landscape mode.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class TabletModePatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "is_tablet"
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
                                    resourceIds[0] -> { // tablet
                                        val insertIndex = index - 2
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        mutableMethod.addInstructions(
                                            index + 3, """
                                                invoke-static {p0}, Lapp/revanced/integrations/settings/MusicSettings;->getTabletMode(Z)Z
                                                move-result p0
                                                """
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
