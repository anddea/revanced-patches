package app.revanced.patches.microg.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.MicroGCompatibility
import app.revanced.util.resources.MicroGResourceUtils.copyFiles

@Patch
@Name("custom-branding-microg-revancify-red")
@Description("Changes the MicroG launcher icon to Revancify Red.")
@MicroGCompatibility
@Version("0.0.1")
class CustomBrandingIconRevancifyRedPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.copyFiles("microg/branding/revancify-red")

        return PatchResultSuccess()
    }

}
