package app.revanced.patches.music.layout.playlistcard.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.litho.patch.MusicLithoFilterPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility

@Patch
@Name("hide-playlist-card")
@Description("Hides the recommand playlist card from home feed.")
@DependsOn(
    [
        MusicLithoFilterPatch::class,
        MusicSettingsPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class HidePlaylistCardPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        MusicSettingsPatch.addMusicPreference("navigation", "revanced_hide_playlist_card", "true")

        return PatchResultSuccess()
    }
}
