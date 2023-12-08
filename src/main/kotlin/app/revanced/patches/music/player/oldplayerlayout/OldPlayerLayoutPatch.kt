package app.revanced.patches.music.player.oldplayerlayout

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.player.oldplayerlayout.fingerprints.OldPlayerLayoutFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception

@Patch(
    name = "Enable old player layout",
    description = "Return the player layout to old style.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.29.58"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object OldPlayerLayoutPatch : BytecodePatch(
    setOf(OldPlayerLayoutFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        OldPlayerLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {}, $PLAYER->enableOldPlayerLayout()Z
                        move-result v0
                        return v0
                        """
                )
            }
        } ?: throw OldPlayerLayoutFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_old_player_layout",
            "false"
        )

    }
}