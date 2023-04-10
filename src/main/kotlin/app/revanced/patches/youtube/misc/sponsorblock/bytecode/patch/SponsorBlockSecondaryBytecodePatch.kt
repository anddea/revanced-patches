package app.revanced.patches.youtube.misc.sponsorblock.bytecode.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.toResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Name("sponsorblock-secondary-bytecode-patch")
@DependsOn([ResourceMappingPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class SponsorBlockSecondaryBytecodePatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "string" to "total_time",
        "layout" to "player_overlays"
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
                                    resourceIds[0] -> { // total time
                                        val targetIndex = index + 2
                                        val targetInstruction = instructions.elementAt(targetIndex)
                                        val targetRegister = (targetInstruction as OneRegisterInstruction).registerA
                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        mutableMethod.addInstructions(
                                            targetIndex + 1, """
                                                invoke-static {v$targetRegister}, Lapp/revanced/integrations/sponsorblock/SegmentPlaybackController;->appendTimeWithoutSegments(Ljava/lang/String;)Ljava/lang/String;
                                                move-result-object v$targetRegister
                                            """
                                        )

                                        patchSuccessArray[0] = true
                                    }

                                    resourceIds[1] -> { // player overlay
                                        val insertIndex = index + 4
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        mutableMethod.addInstruction(
                                            insertIndex,
                                                "invoke-static {p0}, Lapp/revanced/integrations/sponsorblock/ui/SponsorBlockViewController;->initialize(Ljava/lang/Object;)V"
                                        )

                                        patchSuccessArray[1] = true
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
