package app.revanced.patches.youtube.utils.resourceid

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.mapping.ResourceType.ATTR
import app.revanced.patches.shared.mapping.ResourceType.COLOR
import app.revanced.patches.shared.mapping.ResourceType.DIMEN
import app.revanced.patches.shared.mapping.ResourceType.DRAWABLE
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.ResourceType.INTEGER
import app.revanced.patches.shared.mapping.ResourceType.LAYOUT
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.ResourceType.STYLE
import app.revanced.patches.shared.mapping.get
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.patches.shared.mapping.resourceMappings

var accountSwitcherAccessibility = -1L
    private set
var actionBarRingo = -1L
    private set
var actionBarRingoBackground = -1L
    private set
var adAttribution = -1L
    private set
var appearance = -1L
    private set
var appRelatedEndScreenResults = -1L
    private set
var autoNavPreviewStub = -1L
    private set
var autoNavToggle = -1L
    private set
var backgroundCategory = -1L
    private set
var badgeLabel = -1L
    private set
var bar = -1L
    private set
var barContainerHeight = -1L
    private set
var bottomBarContainer = -1L
    private set
var bottomSheetFooterText = -1L
    private set
var bottomSheetRecyclerView = -1L
    private set
var bottomUiContainerStub = -1L
    private set
var captionToggleContainer = -1L
    private set
var castMediaRouteButton = -1L
    private set
var cfFullscreenButton = -1L
    private set
var channelListSubMenu = -1L
    private set
var compactLink = -1L
    private set
var compactListItem = -1L
    private set
var componentLongClickListener = -1L
    private set
var contentPill = -1L
    private set
var controlsLayoutStub = -1L
    private set
var darkBackground = -1L
    private set
var darkSplashAnimation = -1L
    private set
var designBottomSheet = -1L
    private set
var donationCompanion = -1L
    private set
var drawerContentView = -1L
    private set
var drawerResults = -1L
    private set
var easySeekEduContainer = -1L
    private set
var editSettingsAction = -1L
    private set
var endScreenElementLayoutCircle = -1L
    private set
var endScreenElementLayoutIcon = -1L
    private set
var endScreenElementLayoutVideo = -1L
    private set
var emojiPickerIcon = -1L
    private set
var expandButtonDown = -1L
    private set
var fab = -1L
    private set
var fadeDurationFast = -1L
    private set
var filterBarHeight = -1L
    private set
var floatyBarTopMargin = -1L
    private set
var fullScreenButton = -1L
    private set
var fullScreenEngagementOverlay = -1L
    private set
var fullScreenEngagementPanel = -1L
    private set
var horizontalCardList = -1L
    private set
var imageOnlyTab = -1L
    private set
var inlineTimeBarColorizedBarPlayedColorDark = -1L
    private set
var inlineTimeBarPlayedNotHighlightedColor = -1L
    private set
var insetOverlayViewLayout = -1L
    private set
var interstitialsContainer = -1L
    private set
var menuItemView = -1L
    private set
var metaPanel = -1L
    private set
var miniplayerMaxSize = -1L
    private set
var modernMiniPlayerClose = -1L
    private set
var modernMiniPlayerExpand = -1L
    private set
var modernMiniPlayerForwardButton = -1L
    private set
var modernMiniPlayerRewindButton = -1L
    private set
var musicAppDeeplinkButtonView = -1L
    private set
var notice = -1L
    private set
var notificationBigPictureIconWidth = -1L
    private set
var offlineActionsVideoDeletedUndoSnackbarText = -1L
    private set
var playerCollapseButton = -1L
    private set
var playerVideoTitleView = -1L
    private set
var posterArtWidthDefault = -1L
    private set
var qualityAuto = -1L
    private set
var quickActionsElementContainer = -1L
    private set
var reelDynRemix = -1L
    private set
var reelDynShare = -1L
    private set
var reelFeedbackLike = -1L
    private set
var reelFeedbackPause = -1L
    private set
var reelFeedbackPlay = -1L
    private set
var reelForcedMuteButton = -1L
    private set
var reelPlayerFooter = -1L
    private set
var reelPlayerRightPivotV2Size = -1L
    private set
var reelRightDislikeIcon = -1L
    private set
var reelRightLikeIcon = -1L
    private set
var reelTimeBarPlayedColor = -1L
    private set
var reelVodTimeStampsContainer = -1L
    private set
var reelWatchPlayer = -1L
    private set
var relatedChipCloudMargin = -1L
    private set
var rightComment = -1L
    private set
var scrimOverlay = -1L
    private set
var scrubbing = -1L
    private set
var seekEasyHorizontalTouchOffsetToStartScrubbing = -1L
    private set
var seekUndoEduOverlayStub = -1L
    private set
var slidingDialogAnimation = -1L
    private set
var subtitleMenuSettingsFooterInfo = -1L
    private set
var suggestedAction = -1L
    private set
var tapBloomView = -1L
    private set
var titleAnchor = -1L
    private set
var toolTipContentView = -1L
    private set
var totalTime = -1L
    private set
var touchArea = -1L
    private set
var videoQualityBottomSheet = -1L
    private set
var varispeedUnavailableTitle = -1L
    private set
var videoQualityUnavailableAnnouncement = -1L
    private set
var videoZoomSnapIndicator = -1L
    private set
var voiceSearch = -1L
    private set
var youTubeControlsOverlaySubtitleButton = -1L
    private set
var youTubeLogo = -1L
    private set
var ytFillBell = -1L
    private set
var ytOutlinePictureInPictureWhite = -1L
    private set
var ytOutlineVideoCamera = -1L
    private set
var ytOutlineXWhite = -1L
    private set
var ytPremiumWordMarkHeader = -1L
    private set
var ytWordMarkHeader = -1L
    private set


internal val sharedResourceIdPatch = resourcePatch(
    description = "sharedResourceIdPatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        accountSwitcherAccessibility = resourceMappings[
            STRING,
            "account_switcher_accessibility_label"
        ]
        actionBarRingo = resourceMappings[
            LAYOUT,
            "action_bar_ringo"
        ]
        actionBarRingoBackground = resourceMappings[
            LAYOUT,
            "action_bar_ringo_background"
        ]
        adAttribution = resourceMappings[
            ID,
            "ad_attribution"
        ]
        appearance = resourceMappings[
            STRING,
            "app_theme_appearance_dark"
        ]
        appRelatedEndScreenResults = resourceMappings[
            LAYOUT,
            "app_related_endscreen_results"
        ]
        autoNavPreviewStub = resourceMappings[
            ID,
            "autonav_preview_stub"
        ]
        autoNavToggle = resourceMappings[
            ID,
            "autonav_toggle"
        ]
        backgroundCategory = resourceMappings[
            STRING,
            "pref_background_and_offline_category"
        ]
        badgeLabel = resourceMappings[
            ID,
            "badge_label"
        ]
        bar = resourceMappings[
            LAYOUT,
            "bar"
        ]
        barContainerHeight = resourceMappings[
            DIMEN,
            "bar_container_height"
        ]
        bottomBarContainer = resourceMappings[
            ID,
            "bottom_bar_container"
        ]
        bottomSheetFooterText = resourceMappings[
            ID,
            "bottom_sheet_footer_text"
        ]
        bottomSheetRecyclerView = resourceMappings[
            LAYOUT,
            "bottom_sheet_recycler_view"
        ]
        bottomUiContainerStub = resourceMappings[
            ID,
            "bottom_ui_container_stub"
        ]
        captionToggleContainer = resourceMappings[
            ID,
            "caption_toggle_container"
        ]
        castMediaRouteButton = resourceMappings[
            LAYOUT,
            "castmediaroutebutton"
        ]
        cfFullscreenButton = resourceMappings[
            ID,
            "cf_fullscreen_button"
        ]
        channelListSubMenu = resourceMappings[
            LAYOUT,
            "channel_list_sub_menu"
        ]
        compactLink = resourceMappings[
            LAYOUT,
            "compact_link"
        ]
        compactListItem = resourceMappings[
            LAYOUT,
            "compact_list_item"
        ]
        componentLongClickListener = resourceMappings[
            ID,
            "component_long_click_listener"
        ]
        contentPill = resourceMappings[
            LAYOUT,
            "content_pill"
        ]
        controlsLayoutStub = resourceMappings[
            ID,
            "controls_layout_stub"
        ]
        darkBackground = resourceMappings[
            ID,
            "dark_background"
        ]
        darkSplashAnimation = resourceMappings[
            ID,
            "dark_splash_animation"
        ]
        designBottomSheet = resourceMappings[
            ID,
            "design_bottom_sheet"
        ]
        donationCompanion = resourceMappings[
            LAYOUT,
            "donation_companion"
        ]
        drawerContentView = resourceMappings[
            ID,
            "drawer_content_view"
        ]
        drawerResults = resourceMappings[
            ID,
            "drawer_results"
        ]
        easySeekEduContainer = resourceMappings[
            ID,
            "easy_seek_edu_container"
        ]
        editSettingsAction = resourceMappings[
            STRING,
            "edit_settings_action"
        ]
        endScreenElementLayoutCircle = resourceMappings[
            LAYOUT,
            "endscreen_element_layout_circle"
        ]
        endScreenElementLayoutIcon = resourceMappings[
            LAYOUT,
            "endscreen_element_layout_icon"
        ]
        endScreenElementLayoutVideo = resourceMappings[
            LAYOUT,
            "endscreen_element_layout_video"
        ]
        emojiPickerIcon = resourceMappings[
            ID,
            "emoji_picker_icon"
        ]
        expandButtonDown = resourceMappings[
            LAYOUT,
            "expand_button_down"
        ]
        fab = resourceMappings[
            ID,
            "fab"
        ]
        fadeDurationFast = resourceMappings[
            INTEGER,
            "fade_duration_fast"
        ]
        filterBarHeight = resourceMappings[
            DIMEN,
            "filter_bar_height"
        ]
        floatyBarTopMargin = resourceMappings[
            DIMEN,
            "floaty_bar_button_top_margin"
        ]
        fullScreenButton = resourceMappings[
            ID,
            "fullscreen_button"
        ]
        fullScreenEngagementOverlay = resourceMappings[
            LAYOUT,
            "fullscreen_engagement_overlay"
        ]
        fullScreenEngagementPanel = resourceMappings[
            ID,
            "fullscreen_engagement_panel_holder"
        ]
        horizontalCardList = resourceMappings[
            LAYOUT,
            "horizontal_card_list"
        ]
        imageOnlyTab = resourceMappings[
            LAYOUT,
            "image_only_tab"
        ]
        inlineTimeBarColorizedBarPlayedColorDark = resourceMappings[
            COLOR,
            "inline_time_bar_colorized_bar_played_color_dark"
        ]
        inlineTimeBarPlayedNotHighlightedColor = resourceMappings[
            COLOR,
            "inline_time_bar_played_not_highlighted_color"
        ]
        insetOverlayViewLayout = resourceMappings[
            ID,
            "inset_overlay_view_layout"
        ]
        interstitialsContainer = resourceMappings[
            ID,
            "interstitials_container"
        ]
        menuItemView = resourceMappings[
            ID,
            "menu_item_view"
        ]
        metaPanel = resourceMappings[
            ID,
            "metapanel"
        ]
        miniplayerMaxSize = resourceMappings[
            DIMEN,
            "miniplayer_max_size",
        ]
        modernMiniPlayerClose = resourceMappings[
            ID,
            "modern_miniplayer_close"
        ]
        modernMiniPlayerExpand = resourceMappings[
            ID,
            "modern_miniplayer_expand"
        ]
        modernMiniPlayerForwardButton = resourceMappings[
            ID,
            "modern_miniplayer_forward_button"
        ]
        modernMiniPlayerRewindButton = resourceMappings[
            ID,
            "modern_miniplayer_rewind_button"
        ]
        musicAppDeeplinkButtonView = resourceMappings[
            ID,
            "music_app_deeplink_button_view"
        ]
        notice = resourceMappings[
            ID,
            "notice"
        ]
        notificationBigPictureIconWidth = resourceMappings[
            DIMEN,
            "notification_big_picture_icon_width"
        ]
        offlineActionsVideoDeletedUndoSnackbarText = resourceMappings[
            STRING,
            "offline_actions_video_deleted_undo_snackbar_text"
        ]
        playerCollapseButton = resourceMappings[
            ID,
            "player_collapse_button"
        ]
        playerVideoTitleView = resourceMappings[
            ID,
            "player_video_title_view"
        ]
        posterArtWidthDefault = resourceMappings[
            DIMEN,
            "poster_art_width_default"
        ]
        qualityAuto = resourceMappings[
            STRING,
            "quality_auto"
        ]
        quickActionsElementContainer = resourceMappings[
            ID,
            "quick_actions_element_container"
        ]
        reelDynRemix = resourceMappings[
            ID,
            "reel_dyn_remix"
        ]
        reelDynShare = resourceMappings[
            ID,
            "reel_dyn_share"
        ]
        reelFeedbackLike = resourceMappings[
            ID,
            "reel_feedback_like"
        ]
        reelFeedbackPause = resourceMappings[
            ID,
            "reel_feedback_pause"
        ]
        reelFeedbackPlay = resourceMappings[
            ID,
            "reel_feedback_play"
        ]
        reelForcedMuteButton = resourceMappings[
            ID,
            "reel_player_forced_mute_button"
        ]
        reelPlayerFooter = resourceMappings[
            LAYOUT,
            "reel_player_dyn_footer_vert_stories3"
        ]
        reelPlayerRightPivotV2Size = resourceMappings[
            DIMEN,
            "reel_player_right_pivot_v2_size"
        ]
        reelRightDislikeIcon = resourceMappings[
            DRAWABLE,
            "reel_right_dislike_icon"
        ]
        reelRightLikeIcon = resourceMappings[
            DRAWABLE,
            "reel_right_like_icon"
        ]
        reelTimeBarPlayedColor = resourceMappings[
            COLOR,
            "reel_time_bar_played_color"
        ]
        reelVodTimeStampsContainer = resourceMappings[
            ID,
            "reel_vod_timestamps_container"
        ]
        reelWatchPlayer = resourceMappings[
            ID,
            "reel_watch_player"
        ]
        relatedChipCloudMargin = resourceMappings[
            LAYOUT,
            "related_chip_cloud_reduced_margins"
        ]
        rightComment = resourceMappings[
            DRAWABLE,
            "ic_right_comment_32c"
        ]
        scrimOverlay = resourceMappings[
            ID,
            "scrim_overlay"
        ]
        scrubbing = resourceMappings[
            DIMEN,
            "vertical_touch_offset_to_enter_fine_scrubbing"
        ]
        seekEasyHorizontalTouchOffsetToStartScrubbing = resourceMappings[
            DIMEN,
            "seek_easy_horizontal_touch_offset_to_start_scrubbing"
        ]
        seekUndoEduOverlayStub = resourceMappings[
            ID,
            "seek_undo_edu_overlay_stub"
        ]
        slidingDialogAnimation = resourceMappings[
            STYLE,
            "SlidingDialogAnimation"
        ]
        subtitleMenuSettingsFooterInfo = resourceMappings[
            STRING,
            "subtitle_menu_settings_footer_info"
        ]
        suggestedAction = resourceMappings[
            LAYOUT,
            "suggested_action"
        ]
        tapBloomView = resourceMappings[
            ID,
            "tap_bloom_view"
        ]
        titleAnchor = resourceMappings[
            ID,
            "title_anchor"
        ]
        toolTipContentView = resourceMappings[
            LAYOUT,
            "tooltip_content_view"
        ]
        totalTime = resourceMappings[
            STRING,
            "total_time"
        ]
        touchArea = resourceMappings[
            ID,
            "touch_area"
        ]
        videoQualityBottomSheet = resourceMappings[
            LAYOUT,
            "video_quality_bottom_sheet_list_fragment_title"
        ]
        varispeedUnavailableTitle = resourceMappings[
            STRING,
            "varispeed_unavailable_title"
        ]
        videoQualityUnavailableAnnouncement = resourceMappings[
            STRING,
            "video_quality_unavailable_announcement"
        ]
        videoZoomSnapIndicator = resourceMappings[
            ID,
            "video_zoom_snap_indicator"
        ]
        voiceSearch = resourceMappings[
            ID,
            "voice_search"
        ]
        youTubeControlsOverlaySubtitleButton = resourceMappings[
            LAYOUT,
            "youtube_controls_overlay_subtitle_button"
        ]
        youTubeLogo = resourceMappings[
            ID,
            "youtube_logo"
        ]
        ytFillBell = resourceMappings[
            DRAWABLE,
            "yt_fill_bell_black_24"
        ]
        ytOutlinePictureInPictureWhite = resourceMappings[
            DRAWABLE,
            "yt_outline_picture_in_picture_white_24"
        ]
        ytOutlineVideoCamera = resourceMappings[
            DRAWABLE,
            "yt_outline_video_camera_black_24"
        ]
        ytOutlineXWhite = resourceMappings[
            DRAWABLE,
            "yt_outline_x_white_24"
        ]
        ytPremiumWordMarkHeader = resourceMappings[
            ATTR,
            "ytPremiumWordmarkHeader"
        ]
        ytWordMarkHeader = resourceMappings[
            ATTR,
            "ytWordmarkHeader"
        ]
    }
}