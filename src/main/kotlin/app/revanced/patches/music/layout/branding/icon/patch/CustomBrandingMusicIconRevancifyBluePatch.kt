package app.revanced.patches.music.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.resources.IconHelper.customIconMusic

@Patch(false)
@Name("custom-branding-music-revancify-blue")
@Description("Changes the YouTube Music launcher icon to Revancify Blue.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicIconRevancifyBluePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.customIconMusic("revancify-blue")

        return PatchResultSuccess()
    }

}
