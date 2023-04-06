package app.revanced.patches.music.misc.resourceid.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch

@Name("music-resource-id")
@DependsOn([ResourceMappingPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class SharedResourceIdPatch : ResourcePatch {
    internal companion object {
        var colorGreyLabelId: Long = -1
        var dialogSolidLabelId: Long = -1
        var disabledIconLabelId: Long = -1
        var isTabletLabelId: Long = -1
    }

    override fun execute(context: ResourceContext): PatchResult {

        fun findSharedResourceId(type: String, name: String) = ResourceMappingPatch
            .resourceMappings
            .single { it.type == type && it.name == name }.id

        colorGreyLabelId = findSharedResourceId("color", "ytm_color_grey_12")
        dialogSolidLabelId = findSharedResourceId("style", "Theme.YouTubeMusic.Dialog.Solid")
        disabledIconLabelId = findSharedResourceId("dimen", "disabled_icon_alpha")
        isTabletLabelId = findSharedResourceId("bool", "is_tablet")

        return PatchResultSuccess()
    }
}