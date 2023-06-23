package app.revanced.patches.reddit.utils.resourceid.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.util.enum.ResourceType
import app.revanced.util.enum.ResourceType.STRING

@Name("reddit-resource-id")
@DependsOn([ResourceMappingPatch::class])
@RedditCompatibility
@Version("0.0.1")
class SharedResourceIdPatch : ResourcePatch {
    internal companion object {
        var ScreenShotShareBanner: Long = -1
    }

    override fun execute(context: ResourceContext): PatchResult {

        fun find(resourceType: ResourceType, resourceName: String) = ResourceMappingPatch
            .resourceMappings
            .find { it.type == resourceType.value && it.name == resourceName }?.id
            ?: throw PatchResultError("Failed to find resource id : $resourceName")

        ScreenShotShareBanner = find(STRING, "screenshot_share_banner_title")

        return PatchResultSuccess()
    }
}