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
import app.revanced.patches.shared.mapping.ResourceType.XML
import app.revanced.patches.shared.mapping.getResourceId
import app.revanced.patches.shared.mapping.resourceMappingPatch

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
var autoNavScrollCancelPadding = -1L
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
var fullScreenEngagementAdContainer = -1L
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
var inlineTimeBarLiveSeekAbleRange = -1L
    private set
var inlineTimeBarPlayedNotHighlightedColor = -1L
    private set
var insetOverlayViewLayout = -1L
    private set
var interstitialsContainer = -1L
    private set
var insetElementsWrapper = -1L
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
var modernMiniPlayerOverlayActionButton = -1L
    private set
var modernMiniPlayerRewindButton = -1L
    private set
var musicAppDeeplinkButtonView = -1L
    private set
var notificationBigPictureIconWidth = -1L
    private set
var offlineActionsVideoDeletedUndoSnackbarText = -1L
    private set
var playerCollapseButton = -1L
    private set
var playerControlPreviousButtonTouchArea = -1L
    private set
var playerControlNextButtonTouchArea = -1L
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
var seekEasyHorizontalTouchOffsetToStartScrubbing = -1L
    private set
var seekUndoEduOverlayStub = -1L
    private set
var settingsFragment = -1L
    private set
var settingsFragmentCairo = -1L
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
var toolbarContainerId = -1L
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
var verticalTouchOffsetToEnterFineScrubbing = -1L
    private set
var verticalTouchOffsetToStartFineScrubbing = -1L
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
var ytCallToAction = -1L
    private set
var ytFillBell = -1L
    private set
var ytOutlineLibrary = -1L
    private set
var ytOutlineMoonZ = -1L
    private set
var ytOutlinePictureInPictureWhite = -1L
    private set
var ytOutlineVideoCamera = -1L
    private set
var ytOutlineXWhite = -1L
    private set
var ytPremiumWordMarkHeader = -1L
    private set
var ytTextSecondary = -1L
    private set
var ytStaticBrandRed = -1L
    private set
var ytWordMarkHeader = -1L
    private set
var ytYoutubeMagenta = -1L
    private set

internal val sharedResourceIdPatch = resourcePatch(
    description = "sharedResourceIdPatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        accountSwitcherAccessibility = getResourceId(STRING, "account_switcher_accessibility_label")
        actionBarRingo = getResourceId(LAYOUT, "action_bar_ringo")
        actionBarRingoBackground = getResourceId(LAYOUT, "action_bar_ringo_background")
        adAttribution = getResourceId(ID, "ad_attribution")
        appearance = getResourceId(STRING, "app_theme_appearance_dark")
        appRelatedEndScreenResults = getResourceId(LAYOUT, "app_related_endscreen_results")
        autoNavPreviewStub = getResourceId(ID, "autonav_preview_stub")
        autoNavScrollCancelPadding = getResourceId(DIMEN, "autonav_scroll_cancel_padding")
        autoNavToggle = getResourceId(ID, "autonav_toggle")
        backgroundCategory = getResourceId(STRING, "pref_background_and_offline_category")
        badgeLabel = getResourceId(ID, "badge_label")
        bar = getResourceId(LAYOUT, "bar")
        barContainerHeight = getResourceId(DIMEN, "bar_container_height")
        bottomBarContainer = getResourceId(ID, "bottom_bar_container")
        bottomSheetFooterText = getResourceId(ID, "bottom_sheet_footer_text")
        bottomSheetRecyclerView = getResourceId(LAYOUT, "bottom_sheet_recycler_view")
        bottomUiContainerStub = getResourceId(ID, "bottom_ui_container_stub")
        captionToggleContainer = getResourceId(ID, "caption_toggle_container")
        castMediaRouteButton = getResourceId(LAYOUT, "castmediaroutebutton")
        cfFullscreenButton = getResourceId(ID, "cf_fullscreen_button")
        channelListSubMenu = getResourceId(LAYOUT, "channel_list_sub_menu")
        compactLink = getResourceId(LAYOUT, "compact_link")
        compactListItem = getResourceId(LAYOUT, "compact_list_item")
        componentLongClickListener = getResourceId(ID, "component_long_click_listener")
        contentPill = getResourceId(LAYOUT, "content_pill")
        controlsLayoutStub = getResourceId(ID, "controls_layout_stub")
        darkBackground = getResourceId(ID, "dark_background")
        darkSplashAnimation = getResourceId(ID, "dark_splash_animation")
        designBottomSheet = getResourceId(ID, "design_bottom_sheet")
        donationCompanion = getResourceId(LAYOUT, "donation_companion")
        drawerContentView = getResourceId(ID, "drawer_content_view")
        drawerResults = getResourceId(ID, "drawer_results")
        easySeekEduContainer = getResourceId(ID, "easy_seek_edu_container")
        editSettingsAction = getResourceId(STRING, "edit_settings_action")
        endScreenElementLayoutCircle = getResourceId(LAYOUT, "endscreen_element_layout_circle")
        endScreenElementLayoutIcon = getResourceId(LAYOUT, "endscreen_element_layout_icon")
        endScreenElementLayoutVideo = getResourceId(LAYOUT, "endscreen_element_layout_video")
        emojiPickerIcon = getResourceId(ID, "emoji_picker_icon")
        expandButtonDown = getResourceId(LAYOUT, "expand_button_down")
        fab = getResourceId(ID, "fab")
        fadeDurationFast = getResourceId(INTEGER, "fade_duration_fast")
        filterBarHeight = getResourceId(DIMEN, "filter_bar_height")
        floatyBarTopMargin = getResourceId(DIMEN, "floaty_bar_button_top_margin")
        fullScreenButton = getResourceId(ID, "fullscreen_button")
        fullScreenEngagementAdContainer = getResourceId(ID, "fullscreen_engagement_ad_container")
        fullScreenEngagementOverlay = getResourceId(LAYOUT, "fullscreen_engagement_overlay")
        fullScreenEngagementPanel = getResourceId(ID, "fullscreen_engagement_panel_holder")
        horizontalCardList = getResourceId(LAYOUT, "horizontal_card_list")
        imageOnlyTab = getResourceId(LAYOUT, "image_only_tab")
        inlineTimeBarColorizedBarPlayedColorDark =
            getResourceId(COLOR, "inline_time_bar_colorized_bar_played_color_dark")
        inlineTimeBarLiveSeekAbleRange = getResourceId(COLOR, "inline_time_bar_live_seekable_range")
        inlineTimeBarPlayedNotHighlightedColor =
            getResourceId(COLOR, "inline_time_bar_played_not_highlighted_color")
        insetOverlayViewLayout = getResourceId(ID, "inset_overlay_view_layout")
        interstitialsContainer = getResourceId(ID, "interstitials_container")
        insetElementsWrapper = getResourceId(LAYOUT, "inset_elements_wrapper")
        menuItemView = getResourceId(ID, "menu_item_view")
        metaPanel = getResourceId(ID, "metapanel")
        miniplayerMaxSize = getResourceId(DIMEN, "miniplayer_max_size")
        modernMiniPlayerClose = getResourceId(ID, "modern_miniplayer_close")
        modernMiniPlayerExpand = getResourceId(ID, "modern_miniplayer_expand")
        modernMiniPlayerForwardButton = getResourceId(ID, "modern_miniplayer_forward_button")
        modernMiniPlayerOverlayActionButton =
            getResourceId(ID, "modern_miniplayer_overlay_action_button")
        modernMiniPlayerRewindButton = getResourceId(ID, "modern_miniplayer_rewind_button")
        musicAppDeeplinkButtonView = getResourceId(ID, "music_app_deeplink_button_view")
        notificationBigPictureIconWidth =
            getResourceId(DIMEN, "notification_big_picture_icon_width")
        offlineActionsVideoDeletedUndoSnackbarText =
            getResourceId(STRING, "offline_actions_video_deleted_undo_snackbar_text")
        playerCollapseButton = getResourceId(ID, "player_collapse_button")
        playerControlPreviousButtonTouchArea =
            getResourceId(ID, "player_control_previous_button_touch_area")
        playerControlNextButtonTouchArea =
            getResourceId(ID, "player_control_next_button_touch_area")
        playerVideoTitleView = getResourceId(ID, "player_video_title_view")
        posterArtWidthDefault = getResourceId(DIMEN, "poster_art_width_default")
        qualityAuto = getResourceId(STRING, "quality_auto")
        quickActionsElementContainer = getResourceId(ID, "quick_actions_element_container")
        reelDynRemix = getResourceId(ID, "reel_dyn_remix")
        reelDynShare = getResourceId(ID, "reel_dyn_share")
        reelFeedbackLike = getResourceId(ID, "reel_feedback_like")
        reelFeedbackPause = getResourceId(ID, "reel_feedback_pause")
        reelFeedbackPlay = getResourceId(ID, "reel_feedback_play")
        reelForcedMuteButton = getResourceId(ID, "reel_player_forced_mute_button")
        reelPlayerFooter = getResourceId(LAYOUT, "reel_player_dyn_footer_vert_stories3")
        reelPlayerRightPivotV2Size = getResourceId(DIMEN, "reel_player_right_pivot_v2_size")
        reelRightDislikeIcon = getResourceId(DRAWABLE, "reel_right_dislike_icon")
        reelRightLikeIcon = getResourceId(DRAWABLE, "reel_right_like_icon")
        reelTimeBarPlayedColor = getResourceId(COLOR, "reel_time_bar_played_color")
        reelVodTimeStampsContainer = getResourceId(ID, "reel_vod_timestamps_container")
        reelWatchPlayer = getResourceId(ID, "reel_watch_player")
        relatedChipCloudMargin = getResourceId(LAYOUT, "related_chip_cloud_reduced_margins")
        rightComment = getResourceId(DRAWABLE, "ic_right_comment_32c")
        scrimOverlay = getResourceId(ID, "scrim_overlay")
        seekEasyHorizontalTouchOffsetToStartScrubbing =
            getResourceId(DIMEN, "seek_easy_horizontal_touch_offset_to_start_scrubbing")
        seekUndoEduOverlayStub = getResourceId(ID, "seek_undo_edu_overlay_stub")
        settingsFragment = getResourceId(XML, "settings_fragment")
        settingsFragmentCairo = getResourceId(XML, "settings_fragment_cairo")
        slidingDialogAnimation = getResourceId(STYLE, "SlidingDialogAnimation")
        subtitleMenuSettingsFooterInfo = getResourceId(STRING, "subtitle_menu_settings_footer_info")
        suggestedAction = getResourceId(LAYOUT, "suggested_action")
        tapBloomView = getResourceId(ID, "tap_bloom_view")
        titleAnchor = getResourceId(ID, "title_anchor")
        toolbarContainerId = getResourceId(ID, "toolbar_container")
        toolTipContentView = getResourceId(LAYOUT, "tooltip_content_view")
        totalTime = getResourceId(STRING, "total_time")
        touchArea = getResourceId(ID, "touch_area")
        videoQualityBottomSheet =
            getResourceId(LAYOUT, "video_quality_bottom_sheet_list_fragment_title")
        varispeedUnavailableTitle = getResourceId(STRING, "varispeed_unavailable_title")
        verticalTouchOffsetToEnterFineScrubbing =
            getResourceId(DIMEN, "vertical_touch_offset_to_enter_fine_scrubbing")
        verticalTouchOffsetToStartFineScrubbing =
            getResourceId(DIMEN, "vertical_touch_offset_to_start_fine_scrubbing")
        videoQualityUnavailableAnnouncement =
            getResourceId(STRING, "video_quality_unavailable_announcement")
        videoZoomSnapIndicator = getResourceId(ID, "video_zoom_snap_indicator")
        voiceSearch = getResourceId(ID, "voice_search")
        youTubeControlsOverlaySubtitleButton =
            getResourceId(LAYOUT, "youtube_controls_overlay_subtitle_button")
        youTubeLogo = getResourceId(ID, "youtube_logo")
        ytCallToAction = getResourceId(ATTR, "ytCallToAction")
        ytFillBell = getResourceId(DRAWABLE, "yt_fill_bell_black_24")
        ytOutlineLibrary = getResourceId(DRAWABLE, "yt_outline_library_black_24")
        ytOutlineMoonZ = getResourceId(DRAWABLE, "yt_outline_moon_z_vd_theme_24")
        ytOutlinePictureInPictureWhite =
            getResourceId(DRAWABLE, "yt_outline_picture_in_picture_white_24")
        ytOutlineVideoCamera = getResourceId(DRAWABLE, "yt_outline_video_camera_black_24")
        ytOutlineXWhite = getResourceId(DRAWABLE, "yt_outline_x_white_24")
        ytPremiumWordMarkHeader = getResourceId(ATTR, "ytPremiumWordmarkHeader")
        ytTextSecondary = getResourceId(ATTR, "ytTextSecondary")
        ytStaticBrandRed = getResourceId(ATTR, "ytStaticBrandRed")
        ytWordMarkHeader = getResourceId(ATTR, "ytWordmarkHeader")
        ytYoutubeMagenta = getResourceId(COLOR, "yt_youtube_magenta")
    }
}