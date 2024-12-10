package app.revanced.patches.youtube.general.layoutswitch

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.LAYOUT_SWITCH
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/LayoutSwitchPatch;"

@Suppress("unused")
val layoutSwitchPatch = bytecodePatch(
    LAYOUT_SWITCH.title,
    LAYOUT_SWITCH.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        val formFactorEnumClass = formFactorEnumConstructorFingerprint
            .definingClassOrThrow()

        createPlayerRequestBodyWithModelFingerprint.methodOrThrow().apply {
            val index = indexOfFirstInstructionOrThrow {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IGET &&
                        reference?.definingClass == formFactorEnumClass &&
                        reference.type == "I"
            }
            val register = getInstruction<TwoRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                    invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getFormFactor(I)I
                    move-result v$register
                    """
            )
        }

        layoutSwitchFingerprint.methodOrThrow().apply {
            val index = indexOfFirstInstructionReversedOrThrow(Opcode.IF_NEZ)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index, """
                    invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getWidthDp(I)I
                    move-result v$register
                    """
            )
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: LAYOUT_SWITCH"
            ),
            LAYOUT_SWITCH
        )

        // endregion

    }
}
