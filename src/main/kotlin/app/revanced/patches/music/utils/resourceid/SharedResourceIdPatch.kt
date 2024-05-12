package app.revanced.patches.music.utils.resourceid

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.mapping.ResourceMappingPatch
import app.revanced.patches.shared.mapping.ResourceMappingPatch.getId
import app.revanced.patches.shared.mapping.ResourceType.BOOL
import app.revanced.patches.shared.mapping.ResourceType.COLOR
import app.revanced.patches.shared.mapping.ResourceType.DIMEN
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.ResourceType.LAYOUT
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.ResourceType.STYLE

@Patch(dependencies = [ResourceMappingPatch::class])
object SharedResourceIdPatch : ResourcePatch() {
    var AccountSwitcherAccessibility = -1L
    var ButtonContainer = -1L
    var ButtonIconPaddingMedium = -1L
    var ChipCloud = -1L
    var ColorGrey = -1L
    var DesignBottomSheetDialog = -1L
    var DialogSolid = -1L
    var EndButtonsContainer = -1L
    var FloatingLayout = -1L
    var HistoryMenuItem = -1L
    var InlineTimeBarAdBreakMarkerColor = -1L
    var InterstitialsContainer = -1L
    var IsTablet = -1L
    var LikeDislikeContainer = -1L
    var MenuEntry = -1L
    var MiniPlayerDefaultText = -1L
    var MiniPlayerMdxPlaying = -1L
    var MiniPlayerPlayPauseReplayButton = -1L
    var MusicNotifierShelf = -1L
    var MusicTasteBuilderShelf = -1L
    var NamesInactiveAccountThumbnailSize = -1L
    var OfflineSettingsMenuItem = -1L
    var PlayerCastMediaRouteButton = -1L
    var PlayerOverlayChip = -1L
    var PrivacyTosFooter = -1L
    var QualityAuto = -1L
    var RemixGenericButtonSize = -1L
    var SlidingDialogAnimation = -1L
    var Text1 = -1L
    var ToolTipContentView = -1L
    var TopEnd = -1L
    var TopStart = -1L
    var TopBarMenuItemImageView = -1L
    var TosFooter = -1L
    var TouchOutside = -1L
    var TrimSilenceSwitch: Long = -1
    var VarispeedUnavailableTitle = -1L

    override fun execute(context: ResourceContext) {

        AccountSwitcherAccessibility = getId(STRING, "account_switcher_accessibility_label")
        ButtonContainer = getId(ID, "button_container")
        ButtonIconPaddingMedium = getId(DIMEN, "button_icon_padding_medium")
        ChipCloud = getId(LAYOUT, "chip_cloud")
        ColorGrey = getId(COLOR, "ytm_color_grey_12")
        DesignBottomSheetDialog = getId(LAYOUT, "design_bottom_sheet_dialog")
        DialogSolid = getId(STYLE, "Theme.YouTubeMusic.Dialog.Solid")
        EndButtonsContainer = getId(ID, "end_buttons_container")
        FloatingLayout = getId(ID, "floating_layout")
        HistoryMenuItem = getId(ID, "history_menu_item")
        InlineTimeBarAdBreakMarkerColor = getId(COLOR, "inline_time_bar_ad_break_marker_color")
        InterstitialsContainer = getId(ID, "interstitials_container")
        IsTablet = getId(BOOL, "is_tablet")
        LikeDislikeContainer = getId(ID, "like_dislike_container")
        MenuEntry = getId(LAYOUT, "menu_entry")
        MiniPlayerDefaultText = getId(STRING, "mini_player_default_text")
        MiniPlayerMdxPlaying = getId(STRING, "mini_player_mdx_playing")
        MiniPlayerPlayPauseReplayButton = getId(ID, "mini_player_play_pause_replay_button")
        MusicNotifierShelf = getId(LAYOUT, "music_notifier_shelf")
        MusicTasteBuilderShelf = getId(LAYOUT, "music_tastebuilder_shelf")
        NamesInactiveAccountThumbnailSize = getId(DIMEN, "names_inactive_account_thumbnail_size")
        OfflineSettingsMenuItem = getId(ID, "offline_settings_menu_item")
        PlayerCastMediaRouteButton = getId(LAYOUT, "player_cast_media_route_button")
        PlayerOverlayChip = getId(ID, "player_overlay_chip")
        PrivacyTosFooter = getId(ID, "privacy_tos_footer")
        QualityAuto = getId(STRING, "quality_auto")
        RemixGenericButtonSize = getId(DIMEN, "remix_generic_button_size")
        SlidingDialogAnimation = getId(STYLE, "SlidingDialogAnimation")
        Text1 = getId(ID, "text1")
        ToolTipContentView = getId(LAYOUT, "tooltip_content_view")
        TopEnd = getId(ID, "TOP_END")
        TopStart = getId(ID, "TOP_START")
        TopBarMenuItemImageView = getId(ID, "top_bar_menu_item_image_view")
        TosFooter = getId(ID, "tos_footer")
        TouchOutside = getId(ID, "touch_outside")
        TrimSilenceSwitch = getId(ID, "trim_silence_switch")
        VarispeedUnavailableTitle = getId(STRING, "varispeed_unavailable_title")

    }
}