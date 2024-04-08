package app.revanced.patches.youtube.navigation.navigationbuttons

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch

@Patch(
    dependencies = [ResourceMappingPatch::class]
)
internal object NavigationBarHookResourcePatch : ResourcePatch() {
    internal var imageOnlyTabResourceId: Long = -1
    internal var actionBarSearchResultsViewMicId: Long = -1

    override fun execute(context: ResourceContext) {
        imageOnlyTabResourceId = ResourceMappingPatch.resourceMappings.first {
            it.type == "layout" && it.name == "image_only_tab"
        }.id

        actionBarSearchResultsViewMicId = ResourceMappingPatch.resourceMappings.first {
            it.type == "layout" && it.name == "action_bar_search_results_view_mic"
        }.id
    }
}