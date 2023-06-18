package app.revanced.patches.youtube.utils.resourceid.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.util.enum.ResourceType
import app.revanced.util.enum.ResourceType.*

@Name("shared-resource-id")
@DependsOn([ResourceMappingPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class SharedResourceIdPatch : ResourcePatch {
    internal companion object {
        var AccountSwitcherAccessibility: Long = -1
        var ActionBarRingo: Long = -1
        var AdAttribution: Long = -1
        var Appearance: Long = -1
        var AppRelatedEndScreenResults: Long = -1
        var AutoNavPreviewStub: Long = -1
        var BackgroundCategory: Long = -1
        var BarContainerHeight: Long = -1
        var BottomPanelOverlayText: Long = -1
        var BottomUiContainerStub: Long = -1
        var ChannelListSubMenu: Long = -1
        var CompactLink: Long = -1
        var ControlsLayoutStub: Long = -1
        var DislikeButton: Long = -1
        var DonationCompanion: Long = -1
        var EasySeekEduContainer: Long = -1
        var EndScreenElementLayoutCircle: Long = -1
        var EndScreenElementLayoutIcon: Long = -1
        var EndScreenElementLayoutVideo: Long = -1
        var ExpandButtonDown: Long = -1
        var Fab: Long = -1
        var FilterBarHeight: Long = -1
        var FloatyBarTopMargin: Long = -1
        var FullScreenEngagementPanel: Long = -1
        var HorizontalCardList: Long = -1
        var ImageOnlyTab: Long = -1
        var InlineTimeBarColorizedBarPlayedColorDark: Long = -1
        var InlineTimeBarPlayedNotHighlightedColor: Long = -1
        var InsetOverlayViewLayout: Long = -1
        var LiveChatButton: Long = -1
        var QuickActionsElementContainer: Long = -1
        var ReelDynRemix: Long = -1
        var ReelPlayerBadge: Long = -1
        var ReelPlayerBadge2: Long = -1
        var ReelPlayerFooter: Long = -1
        var ReelPlayerInfoPanel: Long = -1
        var ReelPlayerPausedStateButton: Long = -1
        var RelatedChipCloudMargin: Long = -1
        var RightComment: Long = -1
        var ScrimOverlay: Long = -1
        var Scrubbing: Long = -1
        var SearchSuggestionEntry: Long = -1
        var SuggestedAction: Long = -1
        var ToolBarPaddingHome: Long = -1
        var ToolTipContentView: Long = -1
        var TotalTime: Long = -1
        var VideoQualityBottomSheet: Long = -1
        var WatchWhileTimeBarOverlayStub: Long = -1
        var WordMarkHeader: Long = -1
        var YoutubeControlsOverlay: Long = -1
    }

    override fun execute(context: ResourceContext): PatchResult {

        fun find(resourceType: ResourceType, resourceName: String) = ResourceMappingPatch
            .resourceMappings
            .find { it.type == resourceType.value && it.name == resourceName }?.id
            ?: throw PatchResultError("Failed to find resource id : $resourceName")

        AccountSwitcherAccessibility = find(STRING, "account_switcher_accessibility_label")
        ActionBarRingo = find(LAYOUT, "action_bar_ringo")
        AdAttribution = find(ID, "ad_attribution")
        Appearance = find(STRING, "app_theme_appearance_dark")
        AppRelatedEndScreenResults = find(LAYOUT, "app_related_endscreen_results")
        AutoNavPreviewStub = find(ID, "autonav_preview_stub")
        BackgroundCategory = find(STRING, "pref_background_and_offline_category")
        BarContainerHeight = find(DIMEN, "bar_container_height")
        BottomPanelOverlayText = find(ID, "bottom_panel_overlay_text")
        BottomUiContainerStub = find(ID, "bottom_ui_container_stub")
        ChannelListSubMenu = find(LAYOUT, "channel_list_sub_menu")
        CompactLink = find(LAYOUT, "compact_link")
        ControlsLayoutStub = find(ID, "controls_layout_stub")
        DislikeButton = find(ID, "dislike_button")
        DonationCompanion = find(LAYOUT, "donation_companion")
        EasySeekEduContainer = find(ID, "easy_seek_edu_container")
        EndScreenElementLayoutCircle = find(LAYOUT, "endscreen_element_layout_circle")
        EndScreenElementLayoutIcon = find(LAYOUT, "endscreen_element_layout_icon")
        EndScreenElementLayoutVideo = find(LAYOUT, "endscreen_element_layout_video")
        ExpandButtonDown = find(LAYOUT, "expand_button_down")
        Fab = find(ID, "fab")
        FilterBarHeight = find(DIMEN, "filter_bar_height")
        FloatyBarTopMargin = find(DIMEN, "floaty_bar_button_top_margin")
        FullScreenEngagementPanel = find(ID, "fullscreen_engagement_panel_holder")
        HorizontalCardList = find(LAYOUT, "horizontal_card_list")
        ImageOnlyTab = find(LAYOUT, "image_only_tab")
        InlineTimeBarColorizedBarPlayedColorDark = find(COLOR, "inline_time_bar_colorized_bar_played_color_dark")
        InlineTimeBarPlayedNotHighlightedColor = find(COLOR, "inline_time_bar_played_not_highlighted_color")
        InsetOverlayViewLayout = find(ID, "inset_overlay_view_layout")
        LiveChatButton = find(ID, "live_chat_overlay_button")
        QuickActionsElementContainer = find(ID, "quick_actions_element_container")
        ReelDynRemix = find(ID, "reel_dyn_remix")
        ReelPlayerBadge = find(ID, "reel_player_badge")
        ReelPlayerBadge2 = find(ID, "reel_player_badge2")
        ReelPlayerFooter = find(LAYOUT, "reel_player_dyn_footer_vert_stories3")
        ReelPlayerInfoPanel = find(ID, "reel_player_info_panel")
        ReelPlayerPausedStateButton = find(ID, "reel_player_paused_state_buttons")
        RelatedChipCloudMargin = find(LAYOUT, "related_chip_cloud_reduced_margins")
        RightComment = find(DRAWABLE, "ic_right_comment_32c")
        ScrimOverlay = find(ID, "scrim_overlay")
        Scrubbing = find(DIMEN, "vertical_touch_offset_to_enter_fine_scrubbing")
        SearchSuggestionEntry = find(LAYOUT, "search_suggestion_entry")
        SuggestedAction = find(LAYOUT, "suggested_action")
        ToolBarPaddingHome = find(DIMEN, "toolbar_padding_home_action_up")
        ToolTipContentView = find(LAYOUT, "tooltip_content_view")
        TotalTime = find(STRING, "total_time")
        VideoQualityBottomSheet = find(LAYOUT, "video_quality_bottom_sheet_list_fragment_title")
        WatchWhileTimeBarOverlayStub = find(ID, "watch_while_timebar_overlay_stub")
        WordMarkHeader = find(ATTR, "ytWordmarkHeader")
        YoutubeControlsOverlay = find(ID, "youtube_controls_overlay")

        return PatchResultSuccess()
    }
}