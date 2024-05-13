package app.revanced.patches.youtube.general.navigation

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.general.navigation.fingerprints.AutoMotiveFingerprint
import app.revanced.patches.youtube.general.navigation.fingerprints.PivotBarChangedFingerprint
import app.revanced.patches.youtube.general.navigation.fingerprints.PivotBarSetTextFingerprint
import app.revanced.patches.youtube.general.navigation.fingerprints.PivotBarStyleFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.navigation.NavigationBarHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object NavigationBarComponentsPatch : BaseBytecodePatch(
    name = "Navigation bar components",
    description = "Adds options to hide or change components related to navigation bar.",
    dependencies = setOf(
        SettingsPatch::class,
        NavigationBarHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AutoMotiveFingerprint,
        PivotBarChangedFingerprint,
        PivotBarSetTextFingerprint,
        PivotBarStyleFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        // region patch for enable narrow navigation buttons

        arrayOf(
            PivotBarChangedFingerprint,
            PivotBarStyleFingerprint
        ).forEach { fingerprint ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                    val register = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->enableNarrowNavigationButton(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for hide navigation buttons

        AutoMotiveFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getStringInstructionIndex("Android Automotive") - 1
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->switchCreateWithNotificationButton(Z)Z
                        move-result v$register
                        """
                )
            }
        }

        // endregion

        // region patch for hide navigation label

        PivotBarSetTextFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithMethodReferenceName("setText")
                val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        }

        // endregion


        // Hook navigation button created, in order to hide them.
        NavigationBarHookPatch.hookNavigationButtonCreated(GENERAL_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HIDE_NAVIGATION_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
