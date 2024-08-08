package app.revanced.patches.shared.settingmenu

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.integrations.Constants.PATCHES_PATH
import app.revanced.patches.shared.settingmenu.fingerprints.SettingsMenuFingerprint
import app.revanced.patches.shared.viewgroup.ViewGroupMarginLayoutParamsHookPatch
import app.revanced.util.getTargetIndexWithFieldReferenceTypeOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(
    description = "Hide the settings menu for YouTube or YouTube Music.",
    dependencies = [ViewGroupMarginLayoutParamsHookPatch::class]
)
object SettingsMenuPatch : BytecodePatch(
    setOf(SettingsMenuFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$PATCHES_PATH/SettingsMenuPatch;"

    override fun execute(context: BytecodeContext) {

        SettingsMenuFingerprint.resultOrThrow().mutableMethod.apply {
            val insertIndex =
                getTargetIndexWithFieldReferenceTypeOrThrow("Landroid/support/v7/widget/RecyclerView;")
            val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex,
                "invoke-static {v$insertRegister}, " +
                        "$INTEGRATIONS_CLASS_DESCRIPTOR->hideSettingsMenu(Landroid/support/v7/widget/RecyclerView;)V"
            )
        }
    }
}