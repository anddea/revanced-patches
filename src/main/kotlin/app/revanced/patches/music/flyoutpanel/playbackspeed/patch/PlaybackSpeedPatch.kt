package app.revanced.patches.music.flyoutpanel.playbackspeed.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.flyoutpanel.playbackspeed.fingerprints.FlyoutPanelLikeButtonFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.flyoutbutton.patch.FlyoutButtonContainerResourcePatch
import app.revanced.patches.music.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_FLYOUT
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Enable playback speed")
@Description("Add playback speed button to the flyout panel.")
@DependsOn(
    [
        FlyoutButtonContainerResourcePatch::class,
        OverrideSpeedHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class PlaybackSpeedPatch : BytecodePatch(
    listOf(FlyoutPanelLikeButtonFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        FlyoutPanelLikeButtonFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(SharedResourceIdPatch.MusicMenuLikeButtons)

                var insertIndex = -1

                for (index in targetIndex until targetIndex + 5) {
                    if (getInstruction(index).opcode != Opcode.MOVE_RESULT_OBJECT) continue

                    val register = getInstruction<OneRegisterInstruction>(index).registerA
                    insertIndex = index

                    addInstruction(
                        index + 1,
                        "invoke-static {v$register}, $MUSIC_FLYOUT->setFlyoutButtonContainer(Landroid/view/View;)V"
                    )
                    break
                }
                if (insertIndex == -1)
                    throw PatchException("Couldn't find target Index")
            }
        } ?: throw FlyoutPanelLikeButtonFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.FLYOUT,
            "revanced_enable_flyout_panel_playback_speed",
            "false"
        )

    }
}
