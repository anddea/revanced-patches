package app.revanced.patches.music.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.util.resources.IconHelper.customIconMusic

@Patch
@Name("Custom branding icon Revancify blue")
@Description("Changes the YouTube Music launcher icon to Revancify Blue.")
@MusicCompatibility
@Version("0.0.1")
class CustomBrandingIconRevancifyBluePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.customIconMusic("revancify-blue")

        return PatchResultSuccess()
    }

}
