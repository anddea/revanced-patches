package app.revanced.patches.youtube.general.navigation

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.navigation.addBottomBarContainerHook
import app.revanced.patches.youtube.utils.navigation.hookNavigationButtonCreated
import app.revanced.patches.youtube.utils.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList.NAVIGATION_BAR_COMPONENTS
import app.revanced.patches.youtube.utils.playservice.is_19_23_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val navigationBarComponentsPatch = bytecodePatch(
    NAVIGATION_BAR_COMPONENTS.title,
    NAVIGATION_BAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        navigationBarHookPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "SETTINGS: HIDE_NAVIGATION_COMPONENTS"
        )

        // region patch for enable translucent navigation bar

        if (is_19_23_or_greater) {
            translucentNavigationBarFingerprint.injectLiteralInstructionBooleanCall(
                45630927L,
                "$GENERAL_CLASS_DESCRIPTOR->enableTranslucentNavigationBar()Z"
            )

            settingArray += "SETTINGS: TRANSLUCENT_NAVIGATION_BAR"
        }

        // endregion

        // region patch for enable narrow navigation buttons

        arrayOf(
            pivotBarChangedFingerprint,
            pivotBarStyleFingerprint
        ).forEach { fingerprint ->
            fingerprint.matchOrThrow().let {
                it.method.apply {
                    val targetIndex = it.patternMatch!!.startIndex + 1
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

        // region patch for hide navigation bar

        addBottomBarContainerHook("$GENERAL_CLASS_DESCRIPTOR->hideNavigationBar(Landroid/view/View;)V")

        // endregion

        // region patch for hide navigation buttons

        autoMotiveFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstStringInstructionOrThrow("Android Automotive") - 1
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->switchCreateWithNotificationButton(Z)Z
                    move-result v$insertRegister
                    """
            )
        }

        // endregion

        // region patch for hide navigation label

        pivotBarSetTextFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setText"
                }
                val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        }

        // endregion

        // Hook navigation button created, in order to hide them.
        hookNavigationButtonCreated(GENERAL_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, NAVIGATION_BAR_COMPONENTS)

        // endregion
    }
}

