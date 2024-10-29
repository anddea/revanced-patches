package app.revanced.patches.youtube.general.layoutswitch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.shared.fingerprints.CreatePlayerRequestBodyWithModelFingerprint
import app.revanced.patches.youtube.general.layoutswitch.fingerprints.FormFactorEnumConstructorFingerprint
import app.revanced.patches.youtube.general.layoutswitch.fingerprints.LayoutSwitchFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
object LayoutSwitchPatch : BaseBytecodePatch(
    name = "Layout switch",
    description = "Adds an option to spoof the dpi in order to use a tablet or phone layout.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        CreatePlayerRequestBodyWithModelFingerprint,
        FormFactorEnumConstructorFingerprint,
        LayoutSwitchFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR = "$GENERAL_PATH/LayoutSwitchPatch;"

    override fun execute(context: BytecodeContext) {

        val formFactorEnumClass = FormFactorEnumConstructorFingerprint
            .resultOrThrow()
            .mutableMethod
            .definingClass

        CreatePlayerRequestBodyWithModelFingerprint.resultOrThrow().mutableMethod.apply {
            val index = indexOfFirstInstructionOrThrow {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IGET &&
                        reference?.definingClass == formFactorEnumClass &&
                        reference.type == "I"
            }
            val register = getInstruction<TwoRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                    invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->getFormFactor(I)I
                    move-result v$register
                    """
            )
        }

        LayoutSwitchFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val index = indexOfFirstInstructionReversedOrThrow(Opcode.IF_NEZ)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->getWidthDp(I)I
                        move-result v$register
                        """
                )
            }
        }

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
