package app.revanced.patches.music.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.resources.IconHelper.customIconMusic
import app.revanced.util.resources.IconHelper.customIconMusicAdditional

@Patch
@Name("custom-branding-music-afn-red")
@Description("Changes the YouTube Music launcher icon to Afn Red.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicIconAfnRedPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.customIconMusic("afn-red")
        context.customIconMusicAdditional("afn-red")

        return PatchResultSuccess()
    }

}
