package app.revanced.patches.youtube.general.formfactor

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.navigation.hookNavigationButtonCreated
import app.revanced.patches.youtube.utils.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList.CHANGE_FORM_FACTOR
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/ChangeFormFactorPatch;"

@Suppress("unused")
val changeFormFactorPatch = bytecodePatch(
    CHANGE_FORM_FACTOR.title,
    CHANGE_FORM_FACTOR.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        playerTypeHookPatch,
        navigationBarHookPatch,
    )

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

        widthDpUIFingerprint.matchOrThrow().let {
            it.method.apply {
                val index = it.patternMatch!!.startIndex
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getWidthDp(I)I
                        move-result v$register
                        """
                )
            }
        }

        hookNavigationButtonCreated(EXTENSION_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: CHANGE_FORM_FACTOR"
            ),
            CHANGE_FORM_FACTOR
        )

        // endregion

    }
}
