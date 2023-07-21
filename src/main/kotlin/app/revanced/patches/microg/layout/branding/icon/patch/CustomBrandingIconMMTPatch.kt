package app.revanced.patches.microg.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.microg.utils.annotations.MicroGCompatibility
import app.revanced.util.resources.MicroGResourceUtils.copyFiles

@Patch
@Name("custom-branding-microg-mmt")
@Description("Changes the MicroG launcher icon to MMT.")
@MicroGCompatibility
@Version("0.0.1")
class CustomBrandingIconMMTPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.copyFiles("microg/branding/mmt")

        return PatchResultSuccess()
    }

}
