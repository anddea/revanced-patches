package app.revanced.patches.music.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.resources.IconHelper

@Patch
@Name("custom-branding-music-afn-blue")
@Description("Changes the YouTube Music launcher icon (Afn / Blue).")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicPatch_Blue : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        IconHelper.customIconMusic(context, "blue")
        IconHelper.customIconMusicAdditional(context, "blue")

        return PatchResultSuccess()
    }

}
