package app.revanced.patches.music.utils.resourceid.patch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.util.enum.ResourceType
import app.revanced.util.enum.ResourceType.BOOL
import app.revanced.util.enum.ResourceType.COLOR
import app.revanced.util.enum.ResourceType.DIMEN
import app.revanced.util.enum.ResourceType.ID
import app.revanced.util.enum.ResourceType.LAYOUT
import app.revanced.util.enum.ResourceType.STRING
import app.revanced.util.enum.ResourceType.STYLE

@DependsOn([ResourceMappingPatch::class])
class SharedResourceIdPatch : ResourcePatch {
    internal companion object {
        var AccountSwitcherAccessibility: Long = -1
        var ActionsContainer: Long = -1
        var ButtonIconPaddingMedium: Long = -1
        var ChipCloud: Long = -1
        var ColorGrey: Long = -1
        var DialogSolid: Long = -1
        var DisabledIconAlpha: Long = -1
        var InlineTimeBarAdBreakMarkerColor: Long = -1
        var IsTablet: Long = -1
        var MenuEntry: Long = -1
        var MusicMenuLikeButtons: Long = -1
        var NamesInactiveAccountThumbnailSize: Long = -1
        var PlayerOverlayChip: Long = -1
        var PrivacyTosFooter: Long = -1
        var QualityAuto: Long = -1
        var Text1: Long = -1
        var ToolTipContentView: Long = -1
        var TosFooter: Long = -1
    }

    override fun execute(context: ResourceContext) {

        fun find(resourceType: ResourceType, resourceName: String) = ResourceMappingPatch
            .resourceMappings
            .find { it.type == resourceType.value && it.name == resourceName }?.id
            ?: -1

        AccountSwitcherAccessibility = find(STRING, "account_switcher_accessibility_label")
        ActionsContainer = find(ID, "actions_container")
        ButtonIconPaddingMedium = find(DIMEN, "button_icon_padding_medium")
        ChipCloud = find(LAYOUT, "chip_cloud")
        ColorGrey = find(COLOR, "ytm_color_grey_12")
        DialogSolid = find(STYLE, "Theme.YouTubeMusic.Dialog.Solid")
        DisabledIconAlpha = find(DIMEN, "disabled_icon_alpha")
        InlineTimeBarAdBreakMarkerColor = find(COLOR, "inline_time_bar_ad_break_marker_color")
        IsTablet = find(BOOL, "is_tablet")
        MenuEntry = find(LAYOUT, "menu_entry")
        MusicMenuLikeButtons = find(LAYOUT, "music_menu_like_buttons")
        NamesInactiveAccountThumbnailSize = find(DIMEN, "names_inactive_account_thumbnail_size")
        PlayerOverlayChip = find(ID, "player_overlay_chip")
        PrivacyTosFooter = find(ID, "privacy_tos_footer")
        QualityAuto = find(STRING, "quality_auto")
        Text1 = find(ID, "text1")
        ToolTipContentView = find(LAYOUT, "tooltip_content_view")
        TosFooter = find(ID, "tos_footer")

    }
}