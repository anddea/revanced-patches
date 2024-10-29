package app.revanced.patches.music.misc.share

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.misc.share.fingerprints.BottomSheetRecyclerViewFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.BottomSheetRecyclerView
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object ShareSheetPatch : BaseBytecodePatch(
    name = "Change share sheet",
    description = "Add option to change from in-app share sheet to system share sheet.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(BottomSheetRecyclerViewFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/ShareSheetPatch;"

    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ShareSheetMenuFilter;"

    override fun execute(context: BytecodeContext) {

        BottomSheetRecyclerViewFingerprint.resultOrThrow().mutableMethod.apply {
            val constIndex = indexOfFirstWideLiteralInstructionValueOrThrow(BottomSheetRecyclerView)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->onShareSheetMenuCreate(Landroid/support/v7/widget/RecyclerView;)V"
            )
        }

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_change_share_sheet",
            "false"
        )

    }
}