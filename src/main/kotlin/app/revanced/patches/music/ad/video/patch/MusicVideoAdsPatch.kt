package app.revanced.patches.music.ad.video.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.videoads.GeneralVideoAdsPatch
import app.revanced.util.integrations.Constants.MUSIC_SETTINGS_PATH

@Patch
@Name("hide-music-ads")
@Description("Removes ads in the music player.")
@DependsOn(
    [
        GeneralVideoAdsPatch::class,
        MusicSettingsPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicVideoAdsPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        GeneralVideoAdsPatch.injectLegacyAds(INTEGRATIONS_CLASS_DESCRIPTOR)

        GeneralVideoAdsPatch.injectMainstreamAds(INTEGRATIONS_CLASS_DESCRIPTOR)

        return PatchResultSuccess()
    }
    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$MUSIC_SETTINGS_PATH->hideMusicAds()Z"
    }
}
