package app.revanced.patches.music.utils.resourceid.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.util.enum.ResourceType
import app.revanced.util.enum.ResourceType.BOOL
import app.revanced.util.enum.ResourceType.COLOR
import app.revanced.util.enum.ResourceType.DIMEN
import app.revanced.util.enum.ResourceType.ID
import app.revanced.util.enum.ResourceType.LAYOUT
import app.revanced.util.enum.ResourceType.STRING
import app.revanced.util.enum.ResourceType.STYLE

@Name("music-resource-id")
@DependsOn([ResourceMappingPatch::class])
@MusicCompatibility
@Version("0.0.1")
class SharedResourceIdPatch : ResourcePatch {
    internal companion object {
        var ChipCloud: Long = -1
        var ColorGrey: Long = -1
        var DialogSolid: Long = -1
        var DisabledIconAlpha: Long = -1
        var IsTablet: Long = -1
        var MusicNotifierShelf: Long = -1
        var PrivacyTosFooter: Long = -1
        var QualityTitle: Long = -1
        var Text1: Long = -1
    }

    override fun execute(context: ResourceContext): PatchResult {

        fun find(resourceType: ResourceType, resourceName: String) = ResourceMappingPatch
            .resourceMappings
            .find { it.type == resourceType.value && it.name == resourceName }?.id
            ?: throw PatchResultError("Failed to find resource id : $resourceName")

        ChipCloud = find(LAYOUT, "chip_cloud")
        ColorGrey = find(COLOR, "ytm_color_grey_12")
        DialogSolid = find(STYLE, "Theme.YouTubeMusic.Dialog.Solid")
        DisabledIconAlpha = find(DIMEN, "disabled_icon_alpha")
        IsTablet = find(BOOL, "is_tablet")
        MusicNotifierShelf = find(LAYOUT, "music_notifier_shelf")
        PrivacyTosFooter = find(ID, "privacy_tos_footer")
        QualityTitle = find(STRING, "quality_title")
        Text1 = find(ID, "text1")

        return PatchResultSuccess()
    }
}