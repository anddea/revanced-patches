package app.revanced.patches.music.utils.resourceid

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.util.enum.ResourceType
import app.revanced.util.enum.ResourceType.BOOL
import app.revanced.util.enum.ResourceType.COLOR
import app.revanced.util.enum.ResourceType.DIMEN
import app.revanced.util.enum.ResourceType.ID
import app.revanced.util.enum.ResourceType.LAYOUT
import app.revanced.util.enum.ResourceType.STRING
import app.revanced.util.enum.ResourceType.STYLE

@Patch(dependencies = [ResourceMappingPatch::class])
object SharedResourceIdPatch : ResourcePatch() {
    internal var AccountSwitcherAccessibility = -1L
    internal var ActionsContainer = -1L
    internal var ButtonIconPaddingMedium = -1L
    internal var ChipCloud = -1L
    internal var ColorGrey = -1L
    internal var DialogSolid = -1L
    internal var HistoryMenuItem = -1L
    internal var InlineTimeBarAdBreakMarkerColor = -1L
    internal var IsTablet = -1L
    internal var MenuEntry = -1L
    internal var MusicMenuLikeButtons = -1L
    internal var NamesInactiveAccountThumbnailSize = -1L
    internal var PlayerCastMediaRouteButton = -1L
    internal var PlayerOverlayChip = -1L
    internal var PrivacyTosFooter = -1L
    internal var QualityAuto = -1L
    internal var Text1 = -1L
    internal var ToolTipContentView = -1L
    internal var TosFooter = -1L

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
        HistoryMenuItem = find(ID, "history_menu_item")
        InlineTimeBarAdBreakMarkerColor = find(COLOR, "inline_time_bar_ad_break_marker_color")
        IsTablet = find(BOOL, "is_tablet")
        MenuEntry = find(LAYOUT, "menu_entry")
        MusicMenuLikeButtons = find(LAYOUT, "music_menu_like_buttons")
        NamesInactiveAccountThumbnailSize = find(DIMEN, "names_inactive_account_thumbnail_size")
        PlayerCastMediaRouteButton = find(LAYOUT, "player_cast_media_route_button")
        PlayerOverlayChip = find(ID, "player_overlay_chip")
        PrivacyTosFooter = find(ID, "privacy_tos_footer")
        QualityAuto = find(STRING, "quality_auto")
        Text1 = find(ID, "text1")
        ToolTipContentView = find(LAYOUT, "tooltip_content_view")
        TosFooter = find(ID, "tos_footer")

    }
}