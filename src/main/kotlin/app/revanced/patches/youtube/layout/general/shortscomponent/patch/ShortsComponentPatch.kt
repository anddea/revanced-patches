package app.revanced.patches.youtube.layout.general.shortscomponent.patch

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
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("hide-shorts-component")
@Description("Hides other Shorts components.")
@DependsOn(
    [
        LithoFilterPatch::class,
        ResourceMappingPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ShortsComponentPatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "ic_right_comment_32c",
        "reel_dyn_remix",
        "reel_player_paused_state_buttons"
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
                                    resourceIds[0] -> { // shorts player comment
                                        val insertIndex = index - 2
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CONST_HIGH16) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (instructions.elementAt(index + 3) as OneRegisterInstruction).registerA
                                        mutableMethod.implementation!!.injectHideCall(index + 4, viewRegister, "layout/GeneralLayoutPatch", "hideShortsPlayerCommentsButton")

                                        patchSuccessArray[0] = true;
                                    }

                                    resourceIds[1] -> { // shorts player remix
                                        val insertIndex = index - 2
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as Instruction21c).registerA
                                        mutableMethod.implementation!!.injectHideCall(index - 1, viewRegister, "layout/GeneralLayoutPatch", "hideShortsPlayerRemixButton")

                                        patchSuccessArray[1] = true;
                                    }

                                    resourceIds[2] -> { // shorts player subscriptions banner
                                        val insertIndex = index + 3
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as Instruction21c).registerA
                                        mutableMethod.implementation!!.injectHideCall(insertIndex, viewRegister, "layout/GeneralLayoutPatch", "hideShortsPlayerSubscriptionsButton")

                                        patchSuccessArray[2] = true;
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
            context.updatePatchStatus("ShortsComponent")

            /*
            add settings
            */
            SettingsPatch.addPreference(
                arrayOf(
                    "PREFERENCE: LAYOUT_SETTINGS",
                    "PREFERENCE_HEADER: GENERAL",
                    "SETTINGS: SHORTS_COMPONENT.PARENT",
                    "SETTINGS: SHORTS_COMPONENT_PARENT.A",
                    "SETTINGS: SHORTS_COMPONENT_PARENT.B",
                    "SETTINGS: HIDE_SHORTS_COMPONENTS",
                    "SETTINGS: HIDE_SHORTS_SHELF"
                )
            )

            SettingsPatch.updatePatchStatus("hide-shorts-component")

            return PatchResultSuccess()
        } else
            return PatchResultError("Instruction not found: $errorIndex")
    }
}
