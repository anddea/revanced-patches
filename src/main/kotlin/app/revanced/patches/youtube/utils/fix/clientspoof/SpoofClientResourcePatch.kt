package app.revanced.patches.youtube.utils.fix.clientspoof

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.mapping.ResourceMappingPatch

@Patch(dependencies = [ResourceMappingPatch::class])
internal object SpoofClientResourcePatch : ResourcePatch() {
    internal var scrubbedPreviewThumbnailResourceId: Long = -1

    override fun execute(context: ResourceContext) {
        scrubbedPreviewThumbnailResourceId = ResourceMappingPatch[
            "id",
            "thumbnail",
        ]
    }
}
