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

@Patch
@DependsOn(
    [
        GeneralVideoAdsPatch::class,
        MusicIntegrationsPatch::class,
        MusicSettingsPatch::class
    ]
)
@Name("music-video-ads")
@Description("Removes ads in the music player.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicVideoAdsPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {
        val INTEGRATIONS_CLASS_DESCRIPTOR = "Lapp/revanced/integrations/settings/MusicSettings;->getShowAds()Z"

        GeneralVideoAdsPatch.injectLegacyAds(INTEGRATIONS_CLASS_DESCRIPTOR)

        GeneralVideoAdsPatch.injectMainstreamAds(INTEGRATIONS_CLASS_DESCRIPTOR)

        return PatchResultSuccess()
    }
}
