package app.revanced.patches.microg.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.MicroGCompatibility
import app.revanced.util.resources.MicroGResourceUtils.copyFiles

@Patch(false)
@Name("custom-branding-microg-revancify-blue")
@Description("Changes the MicroG launcher icon to Revancify Blue.")
@MicroGCompatibility
@Version("0.0.1")
class CustomBrandingIconRevancifyBluePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.copyFiles("microg/branding/revancify-blue")

        return PatchResultSuccess()
    }

}
