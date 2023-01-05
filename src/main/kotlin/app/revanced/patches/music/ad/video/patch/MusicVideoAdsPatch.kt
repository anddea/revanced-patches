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
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.patches.videoads.GeneralVideoAdsPatch
import app.revanced.shared.util.integrations.Constants.MUSIC_SETTINGS_PATH

@Patch
@DependsOn(
    [
        GeneralVideoAdsPatch::class,
        MusicIntegrationsPatch::class,
        MusicSettingsPatch::class
    ]
)
@Name("hide-music-ads")
@Description("Removes ads in the music player.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicVideoAdsPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {
        val INTEGRATIONS_CLASS_DESCRIPTOR = "$MUSIC_SETTINGS_PATH->hideMusicAds()Z"

        GeneralVideoAdsPatch.injectLegacyAds(INTEGRATIONS_CLASS_DESCRIPTOR)

        GeneralVideoAdsPatch.injectMainstreamAds(INTEGRATIONS_CLASS_DESCRIPTOR)

        return PatchResultSuccess()
    }
}
