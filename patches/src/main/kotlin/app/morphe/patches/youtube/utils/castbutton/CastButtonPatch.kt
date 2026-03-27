@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package app.morphe.patches.youtube.utils.castbutton

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.updatePatchStatus
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/CastButtonPatch;"

private lateinit var playerButtonMethod: MutableMethod
private lateinit var toolbarMenuItemInitializeMethod: MutableMethod
private lateinit var toolbarMenuItemVisibilityMethod: MutableMethod

val castButtonPatch = bytecodePatch(
    description = "castButtonPatch"
) {
    dependsOn(sharedResourceIdPatch)

    execute {
        toolbarMenuItemInitializeMethod = menuItemInitializeFingerprint.methodOrThrow()
        toolbarMenuItemVisibilityMethod =
            menuItemVisibilityFingerprint.methodOrThrow(menuItemInitializeFingerprint)

        playerButtonMethod = playerButtonFingerprint.methodOrThrow()

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

context(BytecodePatchContext)
internal fun hookPlayerCastButton() {
    playerButtonMethod.apply {
        val index = indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.name == "setVisibility"
        }
        val instruction = getInstruction<FiveRegisterInstruction>(index)
        val viewRegister = instruction.registerC
        val visibilityRegister = instruction.registerD
        val reference = getInstruction<ReferenceInstruction>(index).reference

        addInstructions(
            index + 1, """
                    invoke-static {v$visibilityRegister}, $PLAYER_CLASS_DESCRIPTOR->hideCastButton(I)I
                    move-result v$visibilityRegister
                    invoke-virtual {v$viewRegister, v$visibilityRegister}, $reference
                    """
        )
        removeInstruction(index)
    }
    updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "PlayerButtons")
}

context(BytecodePatchContext)
internal fun hookToolBarCastButton() {
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
    updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "ToolBarComponents")
}
