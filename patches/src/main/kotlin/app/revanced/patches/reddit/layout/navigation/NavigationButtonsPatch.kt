package app.revanced.patches.reddit.layout.navigation

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.HIDE_NAVIGATION_BUTTONS
import app.revanced.patches.reddit.utils.settings.is_2024_26_or_greater
import app.revanced.patches.reddit.utils.settings.is_2025_06_or_greater
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/NavigationButtonsPatch;"

@Suppress("unused")
val navigationButtonsPatch = bytecodePatch(
    HIDE_NAVIGATION_BUTTONS.title,
    HIDE_NAVIGATION_BUTTONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        if (is_2024_26_or_greater) {
            val fingerprints = mutableListOf(bottomNavScreenSetupBottomNavigationFingerprint)

            if (is_2025_06_or_greater) fingerprints += composeBottomNavScreenFingerprint

            fingerprints.forEach { fingerprint ->
                fingerprint.methodOrThrow().apply {
                    val arrayIndex = indexOfButtonsArrayInstruction(this)
                    val arrayRegister =
                        getInstruction<OneRegisterInstruction>(arrayIndex + 1).registerA

                    addInstructions(
                        arrayIndex + 2, """
                            invoke-static {v$arrayRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtons([Ljava/lang/Object;)[Ljava/lang/Object;
                            move-result-object v$arrayRegister
                            """
                    )
                }
            }
        } else {
            if (bottomNavScreenFingerprint.resolvable()) {
                val bottomNavScreenMutableClass = with(bottomNavScreenFingerprint.methodOrThrow()) {
                    val startIndex = indexOfGetDimensionPixelSizeInstruction(this)
                    val targetIndex =
                        indexOfFirstInstructionOrThrow(startIndex, Opcode.NEW_INSTANCE)
                    val targetReference =
                        getInstruction<ReferenceInstruction>(targetIndex).reference.toString()

                    classBy { it.type == targetReference }
                        ?.mutableClass
                        ?: throw ClassNotFoundException("Failed to find class $targetReference")
                }

                bottomNavScreenOnGlobalLayoutFingerprint.second.matchOrNull(
                    bottomNavScreenMutableClass
                )
                    ?.let {
                        it.method.apply {
                            val startIndex = it.patternMatch!!.startIndex
                            val targetRegister =
                                getInstruction<FiveRegisterInstruction>(startIndex).registerC

                            addInstruction(
                                startIndex + 1,
                                "invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtons(Landroid/view/ViewGroup;)V"
                            )
                        }
                    }
            } else {
                // Legacy method.
                bottomNavScreenHandlerFingerprint.methodOrThrow().apply {
                    val targetIndex = indexOfGetItemsInstruction(this) + 1
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtons(Ljava/util/List;)Ljava/util/List;
                            move-result-object v$targetRegister
                            """
                    )
                }
            }
        }

        updatePatchStatus(
            "enableNavigationButtons",
            HIDE_NAVIGATION_BUTTONS
        )
    }
}
