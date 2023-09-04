package app.revanced.patches.music.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fix.decoding.patch.DecodingPatch
import app.revanced.util.resources.IconHelper.customIconMusic

@Patch
@Name("Custom branding icon Revancify blue")
@Description("Changes the YouTube Music launcher icon to Revancify Blue.")
@DependsOn([DecodingPatch::class])
@MusicCompatibility
class CustomBrandingIconRevancifyBluePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        context.customIconMusic("revancify-blue")

    }

}
