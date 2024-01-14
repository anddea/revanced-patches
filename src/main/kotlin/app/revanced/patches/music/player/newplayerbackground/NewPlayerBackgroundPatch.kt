package app.revanced.patches.music.player.newplayerbackground

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.player.newplayerbackground.fingerprints.NewPlayerBackgroundFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception

@Patch(
    name = "Enable new player background",
    description = "Adds an option to enable the new player background.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object NewPlayerBackgroundPatch : BytecodePatch(
    setOf(NewPlayerBackgroundFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        NewPlayerBackgroundFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {}, $PLAYER->enableNewPlayerBackground()Z
                        move-result v0
                        return v0
                        """
                )
            }
        } ?: throw NewPlayerBackgroundFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_new_player_background",
            "false"
        )

    }
}