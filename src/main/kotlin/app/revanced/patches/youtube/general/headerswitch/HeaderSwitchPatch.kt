package app.revanced.patches.youtube.general.headerswitch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.WordMarkHeader
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i

@Patch(
    name = "Header switch",
    description = "Add switch to change header.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.34"
            ]
        )
    ]
)
@Suppress("unused")
object HeaderSwitchPatch : BytecodePatch(emptySet()) {
    override fun execute(context: BytecodeContext) {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                method.implementation.apply {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        if (instruction.opcode != Opcode.CONST)
                            return@forEachIndexed
                        // Instruction to store the id WordMarkHeader into a register
                        if ((instruction as Instruction31i).wideLiteral != WordMarkHeader)
                            return@forEachIndexed

                        val targetIndex = index

                    context.proxy(classDef)
                        .mutableClass
                        .findMutableMethodOf(method)
                        .apply {
                            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                            addInstructions(
                                targetIndex + 1, """
                                    invoke-static {v$targetRegister}, $GENERAL->enablePremiumHeader(I)I
                                    move-result v$targetRegister
                                    """
                            )
                        }
                    }
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HEADER_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("Header switch")

    }
}
