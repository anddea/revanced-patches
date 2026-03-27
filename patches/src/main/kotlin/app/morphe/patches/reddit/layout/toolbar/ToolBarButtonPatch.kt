package app.morphe.patches.reddit.layout.toolbar

import app.morphe.patches.reddit.utils.extension.Constants
import app.morphe.patches.reddit.utils.patch.PatchList
import app.morphe.patches.reddit.utils.settings.settingsPatch
import app.morphe.patches.reddit.utils.settings.updatePatchStatus
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_METHOD_DESCRIPTOR =
    "${Constants.PATCHES_PATH}/ToolBarButtonPatch;->hideToolBarButton(Landroid/view/View;)V"

@Suppress("unused")
@Deprecated("This patch is deprecated until Reddit adds a button like r/place or Reddit recap button to the toolbar.")
val toolBarButtonPatch = bytecodePatch {
    // compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        homePagerScreenFingerprint.matchOrThrow().let {
            it.method.apply {
                val stringIndex = it.stringMatches!!.first().index
                val insertIndex = indexOfFirstInstructionOrThrow(stringIndex, Opcode.CHECK_CAST)
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $EXTENSION_METHOD_DESCRIPTOR"
                )
            }
        }

        updatePatchStatus(
            "enableToolBarButton",
            PatchList.HIDE_TOOLBAR_BUTTON
        )
    }
}
