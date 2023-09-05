package app.revanced.patches.music.general.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.util.resources.IconHelper.customIconMusic
import app.revanced.util.resources.IconHelper.customIconMusicAdditional

@Patch(false)
@Name("Custom branding icon MMT")
@Description("Changes the YouTube Music launcher icon to MMT.")
@MusicCompatibility
class CustomBrandingIconMMTPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        context.customIconMusic("mmt")
        context.customIconMusicAdditional("mmt")

    }

}
