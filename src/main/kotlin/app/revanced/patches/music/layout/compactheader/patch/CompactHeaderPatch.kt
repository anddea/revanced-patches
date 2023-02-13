package app.revanced.patches.music.layout.compactheader.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.toResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.util.integrations.Constants.MUSIC_SETTINGS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("hide-compact-header")
@Description("Hides the music category bar at the top of the homepage.")
@DependsOn(
    [
        ResourceMappingPatch::class,
        MusicSettingsPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class CompactHeaderPatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "layout" to "chip_cloud"
    ).map { (type, name) ->
        ResourceMappingPatch
            .resourceMappings
            .single { it.type == type && it.name == name }.id
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
                                    resourceIds[0] -> { // compact header
                                        val insertIndex = index + 4
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val register = (invokeInstruction as Instruction21c).registerA

                                        mutableMethod.addInstruction(
                                            insertIndex,
                                            "invoke-static { v$register }, $MUSIC_SETTINGS_PATH->hideCompactHeader(Landroid/view/View;)V"
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
