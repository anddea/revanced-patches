package app.revanced.patches.reddit.layout.navigation

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.HIDE_NAVIGATION_BUTTONS
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/NavigationButtonsPatch;->hideNavigationButtons(Landroid/view/ViewGroup;)V"

@Suppress("unused")
val navigationButtonsPatch = bytecodePatch(
    HIDE_NAVIGATION_BUTTONS.title,
    HIDE_NAVIGATION_BUTTONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        bottomNavScreenFingerprint.matchOrThrow().let {
            it.method.apply {
                val startIndex = it.patternMatch!!.startIndex
                val targetRegister =
                    getInstruction<FiveRegisterInstruction>(startIndex).registerC

                addInstruction(
                    startIndex + 1,
                    "invoke-static {v$targetRegister}, $EXTENSION_METHOD_DESCRIPTOR"
                )
            }
        }

        updatePatchStatus(
            "enableNavigationButtons",
            HIDE_NAVIGATION_BUTTONS
        )
    }
}
