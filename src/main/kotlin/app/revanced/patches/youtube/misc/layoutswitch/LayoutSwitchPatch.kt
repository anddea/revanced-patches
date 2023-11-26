package app.revanced.patches.youtube.misc.layoutswitch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.misc.layoutswitch.fingerprints.GetFormFactorFingerprint
import app.revanced.patches.youtube.utils.fingerprints.LayoutSwitchFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x

@Patch(
    name = "Layout switch",
    description = "Tricks the dpi to use some tablet/phone layouts.",
    dependencies = [SettingsPatch::class],
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
                "18.45.43"
            ]
        )
    ]
)
@Suppress("unused")
object LayoutSwitchPatch : BytecodePatch(
    setOf(
        GetFormFactorFingerprint,
        LayoutSwitchFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        GetFormFactorFingerprint.result?.let {
            it.mutableMethod.apply {
                val returnCurrentFormFactorIndex = getInstructions().lastIndex - 2

                val returnIsLargeFormFactorLabel = getInstruction(returnCurrentFormFactorIndex - 2)
                val returnFormFactorIndex = getInstruction(returnCurrentFormFactorIndex)

                val insertIndex = returnCurrentFormFactorIndex + 1

                // Replace the labeled instruction with a nop and add the preserved instructions back
                replaceInstruction(returnCurrentFormFactorIndex, BuilderInstruction10x(Opcode.NOP))
                addInstruction(insertIndex, returnFormFactorIndex)

                // Because the labeled instruction is now a nop, we can add our own instructions right after it
                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static { }, $MISC_PATH/LayoutOverridePatch;->enableTabletLayout()Z
                        move-result v0 # Free register
                        if-nez v0, :is_large_form_factor
                        """,
                    ExternalLabel(
                        "is_large_form_factor",
                        returnIsLargeFormFactorLabel
                    )
                )
            }
        } ?: GetFormFactorFingerprint.exception

        LayoutSwitchFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    4, """
                        invoke-static {p0}, $MISC_PATH/LayoutOverridePatch;->getLayoutOverride(I)I
                        move-result p0
                        """
                )
            }
        } ?: throw LayoutSwitchFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: EXPERIMENTAL_FLAGS",
                "SETTINGS: LAYOUT_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("Layout switch")

    }
}
