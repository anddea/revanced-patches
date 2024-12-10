package app.revanced.patches.reddit.layout.toolbar

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.HIDE_TOOLBAR_BUTTON
import app.revanced.patches.reddit.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.reddit.utils.resourceid.toolBarNavSearchCtaContainer
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/ToolBarButtonPatch;->hideToolBarButton(Landroid/view/View;)V"

@Suppress("unused")
@Deprecated("This patch is deprecated until Reddit adds a button like r/place or Reddit recap button to the toolbar.")
val toolBarButtonPatch = bytecodePatch {
    // compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch
    )

    execute {
        homePagerScreenFingerprint.methodOrThrow().apply {
            val targetIndex =
                indexOfFirstLiteralInstructionOrThrow(toolBarNavSearchCtaContainer) + 3
            val targetRegister =
                getInstruction<OneRegisterInstruction>(targetIndex - 1).registerA

            addInstruction(
                targetIndex,
                "invoke-static {v$targetRegister}, $EXTENSION_METHOD_DESCRIPTOR"
            )
        }

        updatePatchStatus(
            "enableToolBarButton",
            HIDE_TOOLBAR_BUTTON
        )
    }
}
