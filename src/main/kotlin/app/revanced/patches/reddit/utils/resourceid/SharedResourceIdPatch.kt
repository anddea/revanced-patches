package app.revanced.patches.reddit.utils.resourceid

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.shared.patch.mapping.ResourceType
import app.revanced.patches.shared.patch.mapping.ResourceType.ID
import app.revanced.patches.shared.patch.mapping.ResourceType.STRING

@Patch(dependencies = [ResourceMappingPatch::class])
object SharedResourceIdPatch : ResourcePatch() {
    var LabelAcknowledgements: Long = -1L
    var ScreenShotShareBanner: Long = -1L
    var ToolBarNavSearchCtaContainer: Long = -1L

    override fun execute(context: ResourceContext) {

        fun find(resourceType: ResourceType, resourceName: String) = ResourceMappingPatch
            .resourceMappings
            .find { it.type == resourceType.value && it.name == resourceName }?.id
            ?: -1

        LabelAcknowledgements = find(STRING, "label_acknowledgements")
        ScreenShotShareBanner = find(STRING, "screenshot_share_banner_title")
        ToolBarNavSearchCtaContainer = find(ID, "toolbar_nav_search_cta_container")

    }
}