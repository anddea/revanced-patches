package app.revanced.patches.music.misc.share

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.extension.Constants.MISC_PATH
import app.revanced.patches.music.utils.patch.PatchList.CHANGE_SHARE_SHEET
import app.revanced.patches.music.utils.patch.PatchList.LITHO_FILTER
import app.revanced.patches.music.utils.resourceid.bottomSheetRecyclerView
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/ShareSheetPatch;"

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ShareSheetMenuFilter;"

@Suppress("unused")
val shareSheetPatch = bytecodePatch(
    CHANGE_SHARE_SHEET.title,
    CHANGE_SHARE_SHEET.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        sharedResourceIdPatch
    )

    execute {
        bottomSheetRecyclerViewFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(bottomSheetRecyclerView)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->onShareSheetMenuCreate(Landroid/support/v7/widget/RecyclerView;)V"
            )
        }

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_change_share_sheet",
            "false"
        )

        updatePatchStatus(CHANGE_SHARE_SHEET)
        updatePatchStatus(LITHO_FILTER)

    }
}
