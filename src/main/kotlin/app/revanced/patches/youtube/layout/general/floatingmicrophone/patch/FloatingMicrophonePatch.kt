package app.revanced.patches.youtube.layout.general.floatingmicrophone.patch

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
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("hide-floating-microphone")
@Description("Hide the floating microphone button above the keyboard.")
@DependsOn(
    [
        ResourceMappingPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class FloatingMicrophonePatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "fab"
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
                                    resourceIds[0] -> { // FloatingActionButton
                                        val insertIndex = index + 4
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.IGET_BOOLEAN) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as TwoRegisterInstruction).registerA

                                        mutableMethod.addInstructions(
                                            insertIndex + 1, """
                                                invoke-static {v$viewRegister}, $GENERAL_LAYOUT->hideFloatingMicrophone(Z)Z
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
                    "PREFERENCE: GENERAL_LAYOUT_SETTINGS",
                    "SETTINGS: HIDE_FLOATING_MICROPHONE"
                )
            )

            SettingsPatch.updatePatchStatus("hide-floating-microphone")

            return PatchResultSuccess()
        } else
            return PatchResultError("Instruction not found: $errorIndex")
    }
}
