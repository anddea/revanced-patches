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
import app.revanced.util.enum.ResourceType
import app.revanced.util.enum.ResourceType.*

@Name("music-resource-id")
@DependsOn([ResourceMappingPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class SharedResourceIdPatch : ResourcePatch {
    internal companion object {
        var colorGreyLabelId: Long = -1
        var dialogSolidLabelId: Long = -1
        var disabledIconLabelId: Long = -1
        var floatingActionButtonLabelId: Long = -1
        var isTabletLabelId: Long = -1
    }

    override fun execute(context: ResourceContext): PatchResult {

        fun find(type: ResourceType, name: String) = ResourceMappingPatch
            .resourceMappings
            .single { it.type == type.value && it.name == name }.id

        colorGreyLabelId = find(COLOR, "ytm_color_grey_12")
        dialogSolidLabelId = find(STYLE, "Theme.YouTubeMusic.Dialog.Solid")
        disabledIconLabelId = find(DIMEN, "disabled_icon_alpha")
        floatingActionButtonLabelId = find(ID, "floating_action_button")
        isTabletLabelId = find(BOOL, "is_tablet")

        return PatchResultSuccess()
    }
}