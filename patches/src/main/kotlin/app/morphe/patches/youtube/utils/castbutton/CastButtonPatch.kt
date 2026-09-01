@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package app.morphe.patches.youtube.utils.castbutton

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.playservice.is_21_04_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.util.findMethodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.updatePatchStatus
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/CastButtonPatch;"

private lateinit var toolbarMenuItemInitializeMethod: MutableMethod
private lateinit var toolbarMenuItemVisibilityMethod: MutableMethod
private var modernToolbarMenuItem = false

val castButtonPatch = bytecodePatch(
    description = "castButtonPatch"
) {
    dependsOn(sharedResourceIdPatch, versionCheckPatch)

    execute {
        modernToolbarMenuItem = is_21_04_or_greater
        toolbarMenuItemInitializeMethod = MenuItemInitializeFingerprint.method
        toolbarMenuItemVisibilityMethod = if (modernToolbarMenuItem) {
            ModernMenuItemVisibilityFingerprint.method
        } else {
            MenuItemVisibilityFingerprint.method
        }

        findMethodOrThrow("Landroidx/mediarouter/app/MediaRouteButton;") {
            name == "setVisibility"
        }.addInstructions(
            0, """
                invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->hideCastButton(I)I
                move-result p1
                """
        )
    }
}

context(_: BytecodePatchContext)
internal fun hookToolBarCastButton() {
    if (modernToolbarMenuItem) {
        // YouTube 21.04+ keeps the MenuItem register in the setShowAsAction invoke.
        toolbarMenuItemInitializeMethod.apply {
            val index = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "setShowAsAction"
            }
            val menuItemRegister = getInstruction<FiveRegisterInstruction>(index).registerC
            addInstruction(
                index,
                "invoke-static { v$menuItemRegister }, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Landroid/view/MenuItem;)V"
            )
        }

        toolbarMenuItemVisibilityMethod.apply {
            val index = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "setVisible"
            }
            val visibilityRegister = getInstruction<FiveRegisterInstruction>(index).registerD
            addInstructions(
                index,
                """
                    invoke-static { v$visibilityRegister }, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Z)Z
                    move-result v$visibilityRegister
                """
            )
        }
    } else {
        toolbarMenuItemInitializeMethod.apply {
            val index = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "setShowAsAction"
            } + 1
            addInstruction(
                index,
                "invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Landroid/view/MenuItem;)V"
            )
        }
        toolbarMenuItemVisibilityMethod.addInstructions(
            0, """
                    invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Z)Z
                    move-result p1
                    """
        )
    }
    updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "ToolBarComponents")
}
