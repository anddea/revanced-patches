package app.revanced.patches.youtube.layout.player.suggestactions.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.injectHideCall
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.misc.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.formats.Instruction22c
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("hide-suggested-actions")
@Description("Hide the suggested actions bar inside the player.")
@DependsOn(
    [
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class,
        ResourceMappingPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SuggestedActionsPatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "suggested_action"
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
                                    resourceIds[0] -> { // suggested_action
                                        val insertIndex = index + 4
                                        val iPutInstruction = instructions.elementAt(insertIndex)
                                        if (iPutInstruction.opcode != Opcode.IPUT_OBJECT) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (iPutInstruction as Instruction22c).registerA
                                        mutableMethod.implementation!!.injectHideCall(insertIndex, viewRegister, "layout/PlayerPatch", "hideSuggestedActions")

                                        patchSuccessArray[0] = true
                                    }
                                }
                            }
                            else -> return@forEachIndexed
                        }
                    }
                }
            }
        }

        val errorIndex: Int = patchSuccessArray.indexOf(false)

        if (errorIndex == -1) {
            context.updatePatchStatus("SuggestedActions")

            /*
             * Add settings
             */
            SettingsPatch.addPreference(
                arrayOf(
                    "PREFERENCE: PLAYER_SETTINGS",
                    "SETTINGS: HIDE_SUGGESTED_ACTION"
                )
            )

            SettingsPatch.updatePatchStatus("hide-suggested-actions")

            return PatchResultSuccess()
        } else
            return PatchResultError("Instruction not found: $errorIndex")
    }
}
