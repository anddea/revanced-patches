package app.revanced.patches.youtube.utils.castbutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.castbutton.fingerprints.MenuItemInitializeFingerprint
import app.revanced.patches.youtube.utils.castbutton.fingerprints.MenuItemVisibilityFingerprint
import app.revanced.patches.youtube.utils.castbutton.fingerprints.PlayerButtonFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.alsoResolve
import app.revanced.util.findMethodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.resultOrThrow
import app.revanced.util.updatePatchStatus
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(dependencies = [SharedResourceIdPatch::class])
object CastButtonPatch : BytecodePatch(
    setOf(
        MenuItemInitializeFingerprint,
        PlayerButtonFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$UTILS_PATH/CastButtonPatch;"

    private lateinit var playerButtonMethod: MutableMethod
    private lateinit var toolbarMenuItemInitializeMethod: MutableMethod
    private lateinit var toolbarMenuItemVisibilityMethod: MutableMethod

    override fun execute(context: BytecodeContext) {

        toolbarMenuItemInitializeMethod = MenuItemInitializeFingerprint.resultOrThrow()
            .mutableMethod
        toolbarMenuItemVisibilityMethod = MenuItemVisibilityFingerprint.alsoResolve(
            context, MenuItemInitializeFingerprint
        ).mutableMethod

        playerButtonMethod = PlayerButtonFingerprint.resultOrThrow().mutableMethod

        context.findMethodOrThrow("Landroidx/mediarouter/app/MediaRouteButton;") {
            name == "setVisibility"
        }.addInstructions(
            0, """
                invoke-static {p1}, $INTEGRATIONS_CLASS_DESCRIPTOR->hideCastButton(I)I
                move-result p1
                """
        )

    }

    internal fun hookPlayerButton(context: BytecodeContext) {
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
        context.updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "PlayerButtons")
    }

    internal fun hookToolBarButton(context: BytecodeContext) {
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
        context.updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "ToolBarComponents")
    }
}