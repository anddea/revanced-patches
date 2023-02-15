package app.revanced.patches.youtube.layout.fullscreen.endscreenoverlay.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN_LAYOUT
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("hide-endscreen-overlay")
@Description("Hide endscreen overlay on swipe controls.")
@DependsOn(
    [
        ResourceMappingPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideEndscreenOverlayPatch : BytecodePatch() {
    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "app_related_endscreen_results"
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
                                    resourceIds[0] -> { // end screen result
                                        val insertIndex = index - 13
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.IF_NEZ) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val dummyRegister = (instructions.elementAt(index) as Instruction31i).registerA
                                        mutableMethod.addInstructions(
                                            insertIndex, """
                                                invoke-static {}, $FULLSCREEN_LAYOUT->hideEndscreenOverlay()Z
                                                move-result v$dummyRegister
                                                if-eqz v$dummyRegister, :on
                                                return-void
                                            """, listOf(ExternalLabel("on", mutableMethod.instruction(insertIndex)))
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

        val errorIndex: Int = patchSuccessArray.indexOf(false)

        if (errorIndex == -1) {
            /*
            add settings
            */
            SettingsPatch.addPreference(
                arrayOf(
                    "PREFERENCE: LAYOUT_SETTINGS",
                    "PREFERENCE_HEADER: FULLSCREEN",
                    "SETTINGS: HIDE_ENDSCREEN_OVERLAY"
                )
            )

            SettingsPatch.updatePatchStatus("hide-endscreen-overlay")

            return PatchResultSuccess()
        } else
            return PatchResultError("Instruction not found: $errorIndex")
    }
}
