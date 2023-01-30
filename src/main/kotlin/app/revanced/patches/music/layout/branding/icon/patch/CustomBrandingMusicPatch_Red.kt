package app.revanced.patches.music.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.util.resources.IconHelper

@Patch
@Name("custom-branding-music-afn-red")
@Description("Changes the YouTube Music launcher icon (Afn / Red).")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicPatch_Red : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        IconHelper.customIconMusic(context, "red")
        IconHelper.customIconMusicAdditional(context, "red")

        return PatchResultSuccess()
    }

}
