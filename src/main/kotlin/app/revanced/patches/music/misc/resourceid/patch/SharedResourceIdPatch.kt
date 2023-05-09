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
        var chipCloudId: Long = -1
        var colorGreyId: Long = -1
        var dialogSolidId: Long = -1
        var disabledIconId: Long = -1
        var isTabletId: Long = -1
        var notifierShelfId: Long = -1
        var privacyTosFooterId: Long = -1
        var qualityAutoId: Long = -1
        var qualityTitleId: Long = -1
    }

    override fun execute(context: ResourceContext): PatchResult {

        fun find(type: ResourceType, name: String) = ResourceMappingPatch
            .resourceMappings
            .single { it.type == type.value && it.name == name }.id

        chipCloudId = find(LAYOUT, "chip_cloud")
        colorGreyId = find(COLOR, "ytm_color_grey_12")
        dialogSolidId = find(STYLE, "Theme.YouTubeMusic.Dialog.Solid")
        disabledIconId = find(DIMEN, "disabled_icon_alpha")
        isTabletId = find(BOOL, "is_tablet")
        notifierShelfId = find(LAYOUT, "music_notifier_shelf")
        privacyTosFooterId = find(ID, "privacy_tos_footer")
        qualityAutoId = find(STRING, "quality_auto")
        qualityTitleId = find(STRING, "quality_title")

        return PatchResultSuccess()
    }
}