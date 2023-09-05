package app.revanced.patches.music.flyoutpanel.hide.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.flyoutpanel.hide.fingerprints.MenuItemFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.flyoutbuttonhook.patch.FlyoutButtonHookPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_FLYOUT
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Hide flyout panel")
@Description("Hides flyout panel components.")
@DependsOn(
    [
        FlyoutButtonHookPatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
class FlyoutPanelPatch : BytecodePatch(
    listOf(MenuItemFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        MenuItemFingerprint.result?.let {
            it.mutableMethod.apply {
                val freeIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.OR_INT_LIT16
                }
                val freeRegister = getInstruction<TwoRegisterInstruction>(freeIndex).registerA

                val targetIndex = it.scanResult.patternScanResult!!.startIndex + 3
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                val jumpInstruction =
                    getInstruction<Instruction>(implementation!!.instructions.size - 1)

                addInstructionsWithLabels(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $MUSIC_FLYOUT->hideFlyoutPanels(Ljava/lang/Enum;)Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :hide
                        """, ExternalLabel("hide", jumpInstruction)
                )
            }
        } ?: throw MenuItemFingerprint.exception

        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_add_to_queue",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_dismiss_queue",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_download",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_go_to_album",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_go_to_artist",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_like_dislike",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_play_next",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_remove_from_library",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_report",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_save_to_library",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_save_to_playlist",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_share",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_start_radio",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithoutSummary(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_panel_view_song_credit",
            "false"
        )

    }
}
