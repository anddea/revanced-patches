package app.revanced.patches.youtube.layout.seekbar.speed.patch

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
import app.revanced.patches.youtube.misc.overridespeed.bytecode.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction35c
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("enable-timestamps-speed")
@Description("Add the current video speed in brackets next to the current time.")
@DependsOn(
    [
        OverrideSpeedHookPatch::class,
        ResourceMappingPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class AppendSpeedPatch : BytecodePatch() {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "string" to "total_time"
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
                    this?.instructions?.forEachIndexed { _, instruction ->
                        when (instruction.opcode) {
                            Opcode.CONST -> {
                                when ((instruction as Instruction31i).wideLiteral) {
                                    resourceIds[0] -> { // total time
                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        for ((targetIndex, targetInstruction) in instructions.withIndex()) {
                                            if (targetInstruction.opcode != Opcode.INVOKE_VIRTUAL) continue

                                            if ((targetInstruction as DexBackedInstruction35c).reference.toString() ==
                                                "Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;") {
                                                val insertIndex = targetIndex + 2
                                                val insertRegister = (instructions.elementAt(insertIndex) as Instruction35c).registerC
                                                mutableMethod.addInstructions(
                                                    insertIndex, """
                                                        invoke-static {v$insertRegister}, $SEEKBAR->enableTimeStampSpeed(Ljava/lang/String;)Ljava/lang/String;
                                                        move-result-object v$insertRegister
                                                        """
                                                )
                                                patchSuccessArray[0] = true
                                                break
                                            }
                                        }
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
                    "PREFERENCE: SEEKBAR_SETTINGS",
                    "SETTINGS: ENABLE_TIME_STAMP_SPEED"
                )
            )

            SettingsPatch.updatePatchStatus("enable-timestamps-speed")

            return PatchResultSuccess()
        } else
            return PatchResultError("Instruction not found: $errorIndex")
    }
}
