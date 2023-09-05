package app.revanced.patches.reddit.utils.resourceid.patch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.util.enum.ResourceType
import app.revanced.util.enum.ResourceType.STRING

@DependsOn([ResourceMappingPatch::class])
class SharedResourceIdPatch : ResourcePatch {
    internal companion object {
        var ScreenShotShareBanner: Long = -1
    }

    override fun execute(context: ResourceContext) {

        fun find(resourceType: ResourceType, resourceName: String) = ResourceMappingPatch
            .resourceMappings
            .find { it.type == resourceType.value && it.name == resourceName }?.id
            ?: -1

        ScreenShotShareBanner = find(STRING, "screenshot_share_banner_title")

    }
}