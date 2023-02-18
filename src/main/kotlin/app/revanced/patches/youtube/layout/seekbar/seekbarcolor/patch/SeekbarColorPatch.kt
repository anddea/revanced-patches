package app.revanced.patches.youtube.layout.seekbar.seekbarcolor.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR_LAYOUT
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("custom-seekbar-color")
@Description("Change seekbar color in dark mode.")
@DependsOn(
    [
        ResourceMappingPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SeekbarColorPatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "inline_time_bar_colorized_bar_played_color_dark"
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
                                    resourceIds[0] -> { // seekbar color
                                        val registerIndex = index + 2

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (instructions.elementAt(registerIndex) as OneRegisterInstruction).registerA

                                        mutableMethod.addInstructions(
                                            registerIndex + 1, """
                                                invoke-static {v$viewRegister}, $SEEKBAR_LAYOUT->enableCustomSeekbarColor(I)I
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

        val errorIndex: Int = patchSuccessArray.indexOf(false)

        if (errorIndex == -1) {
            /*
             * Add settings
             */
            SettingsPatch.addPreference(
                arrayOf(
                    "PREFERENCE: OTHER_LAYOUT_SETTINGS",
                    "PREFERENCE_HEADER: SEEKBAR",
                    "SETTINGS: CUSTOM_SEEKBAR_COLOR"
                )
            )

            SettingsPatch.updatePatchStatus("custom-seekbar-color")

            return PatchResultSuccess()
        } else
            return PatchResultError("Instruction not found: $errorIndex")
    }
}
