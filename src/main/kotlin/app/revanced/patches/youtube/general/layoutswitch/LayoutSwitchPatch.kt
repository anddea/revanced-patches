package app.revanced.patches.youtube.general.layoutswitch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.general.layoutswitch.fingerprints.GetFormFactorFingerprint
import app.revanced.patches.youtube.general.layoutswitch.fingerprints.LayoutSwitchFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object LayoutSwitchPatch : BaseBytecodePatch(
    name = "Layout switch",
    description = "Adds an option to trick dpi to use tablet or phone layout.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        GetFormFactorFingerprint,
        LayoutSwitchFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        // region patch for enable tablet layout

        GetFormFactorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val jumpIndex = getTargetIndexReversed(Opcode.SGET_OBJECT)

                addInstructionsWithLabels(
                    0, """
                        invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->enableTabletLayout()Z
                        move-result v0 # Free register
                        if-nez v0, :is_large_form_factor
                        """,
                    ExternalLabel(
                        "is_large_form_factor",
                        getInstruction(jumpIndex)
                    )
                )
            }
        }

        // endregion

        // region patch for enable phone layout

        LayoutSwitchFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndex(Opcode.IF_NEZ)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->enablePhoneLayout(I)I
                        move-result v$insertRegister
                        """
                )
            }
        }

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: LAYOUT_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
