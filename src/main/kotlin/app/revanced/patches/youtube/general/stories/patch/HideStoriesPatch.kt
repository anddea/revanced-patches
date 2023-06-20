package app.revanced.patches.youtube.general.stories.patch

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
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.formats.Instruction22c
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("hide-stories")
@Description("Hides YouTube Stories shelf on the feed.")
@DependsOn(
    [
        ResourceMappingPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideStoriesPatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "reel_multiple_items_shelf",
        "reel_item_container",
        "reel_multiple_items_shelf_title_layout"
    ).map { name ->
        ResourceMappingPatch.resourceMappings.single { it.name == name }.id
    }
    private var patchSuccessArray = Array(resourceIds.size) { false }

    override fun execute(context: BytecodeContext): PatchResult {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                with(method.implementation) {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        when (instruction.opcode) {
                            Opcode.CONST -> {
                                when ((instruction as Instruction31i).wideLiteral) {
                                    resourceIds[0], resourceIds[1] -> {
                                        val insertIndex = index + 4
                                        val iPutInstruction = instructions.elementAt(insertIndex)
                                        if (iPutInstruction.opcode != Opcode.IPUT_OBJECT) return@forEachIndexed

                                        val mutableMethod =
                                            context.proxy(classDef).mutableClass.findMutableMethodOf(
                                                method
                                            )

                                        val viewRegister =
                                            (iPutInstruction as Instruction22c).registerA
                                        mutableMethod.implementation!!.injectHideCall(
                                            insertIndex,
                                            viewRegister,
                                            "layout/GeneralPatch",
                                            "hideStoriesShelf"
                                        )

                                        patchSuccessArray[0] = true
                                        patchSuccessArray[1] = true
                                    }

                                    resourceIds[2] -> {
                                        val insertIndex = index - 1
                                        val iPutInstruction = instructions.elementAt(insertIndex)
                                        if (iPutInstruction.opcode != Opcode.IPUT_OBJECT) return@forEachIndexed

                                        val mutableMethod =
                                            context.proxy(classDef).mutableClass.findMutableMethodOf(
                                                method
                                            )

                                        val viewRegister =
                                            (iPutInstruction as Instruction22c).registerA
                                        mutableMethod.implementation!!.injectHideCall(
                                            insertIndex,
                                            viewRegister,
                                            "layout/GeneralPatch",
                                            "hideStoriesShelf"
                                        )

                                        patchSuccessArray[2] = true
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
            /*
             * Add settings
             */
            SettingsPatch.addPreference(
                arrayOf(
                    "PREFERENCE: GENERAL_SETTINGS",
                    "SETTINGS: HIDE_STORIES_SHELF"
                )
            )

            SettingsPatch.updatePatchStatus("hide-stories")

            return PatchResultSuccess()
        } else
            return PatchResultError("Instruction not found: $errorIndex")
    }
}
