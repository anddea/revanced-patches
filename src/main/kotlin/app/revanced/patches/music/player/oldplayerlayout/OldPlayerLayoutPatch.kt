package app.revanced.patches.music.player.oldplayerlayout

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.fingerprints.NewPlayerLayoutFingerprint
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_PLAYER

@Patch(
    name = "Enable old player layout",
    description = "Return the player layout to old style.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")],
    use = false
)
@Suppress("unused")
object OldPlayerLayoutPatch : BytecodePatch(
    setOf(NewPlayerLayoutFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        NewPlayerLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {}, $MUSIC_PLAYER->enableOldPlayerLayout()Z
                        move-result v0
                        return v0
                        """
                )
            }
        } ?: throw NewPlayerLayoutFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_old_player_layout",
            "false"
        )

    }
}