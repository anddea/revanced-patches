package app.revanced.patches.music.player.minimizedplayer.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.player.minimizedplayer.fingerprints.MinimizedPlayerFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Enable force minimized player")
@Description("Permanently keep player minimized even if another track is played.")
@DependsOn([SettingsPatch::class])
@MusicCompatibility
class MinimizedPlayerPatch : BytecodePatch(
    listOf(MinimizedPlayerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        MinimizedPlayerFingerprint.result?.let {
            with(it.mutableMethod) {
                val index = it.scanResult.patternScanResult!!.endIndex
                val register =
                    (implementation!!.instructions[index] as OneRegisterInstruction).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $MUSIC_PLAYER->enableForceMinimizedPlayer(Z)Z
                        move-result v$register
                        """
                )
            }
        } ?: throw MinimizedPlayerFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_force_minimized_player",
            "true"
        )

    }
}