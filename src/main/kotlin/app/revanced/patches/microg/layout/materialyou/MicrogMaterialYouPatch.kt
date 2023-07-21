package app.revanced.patches.microg.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.microg.utils.annotations.MicroGCompatibility

import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources

@Patch(false)
@Name("microg-materialyou")
@Description("Enables MaterialYou theme for microG for Android 12+")
@MicroGCompatibility
@Version("0.0.1")
class MicrogMaterialYouPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        arrayOf(
            ResourceUtils.ResourceGroup(
                "values-v31",
                "styles.xml"
            ),
        ).forEach {
            context["res/${it.resourceDirectoryName}"].mkdirs()
            context.copyResources("microg/materialyou", it)
        }

        return PatchResultSuccess()
    }
}