package app.revanced.patches.music.utils.resourceid

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.mapping.ResourceType.BOOL
import app.revanced.patches.shared.mapping.ResourceType.COLOR
import app.revanced.patches.shared.mapping.ResourceType.DIMEN
import app.revanced.patches.shared.mapping.ResourceType.DRAWABLE
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.ResourceType.LAYOUT
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.ResourceType.STYLE
import app.revanced.patches.shared.mapping.get
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.patches.shared.mapping.resourceMappings

var accountSwitcherAccessibility = -1L
    private set
var actionBarLogo = -1L
    private set
var actionBarLogoRingo2 = -1L
    private set
var bottomSheetRecyclerView = -1L
    private set
var buttonContainer = -1L
    private set
var buttonIconPaddingMedium = -1L
    private set
var chipCloud = -1L
    private set
var colorGrey = -1L
    private set
var darkBackground = -1L
    private set
var designBottomSheetDialog = -1L
    private set
var endButtonsContainer = -1L
    private set
var floatingLayout = -1L
    private set
var historyMenuItem = -1L
    private set
var inlineTimeBarAdBreakMarkerColor = -1L
    private set
var interstitialsContainer = -1L
    private set
var isTablet = -1L
    private set
var likeDislikeContainer = -1L
    private set
var mainActivityLaunchAnimation = -1L
    private set
var menuEntry = -1L
    private set
var miniPlayerDefaultText = -1L
    private set
var miniPlayerMdxPlaying = -1L
    private set
var miniPlayerPlayPauseReplayButton = -1L
    private set
var miniPlayerViewPager = -1L
    private set
var musicNotifierShelf = -1L
    private set
var musicTasteBuilderShelf = -1L
    private set
var namesInactiveAccountThumbnailSize = -1L
    private set
var offlineSettingsMenuItem = -1L
    private set
var playerOverlayChip = -1L
    private set
var playerViewPager = -1L
    private set
var privacyTosFooter = -1L
    private set
var qualityAuto = -1L
    private set
var remixGenericButtonSize = -1L
    private set
var slidingDialogAnimation = -1L
    private set
var tapBloomView = -1L
    private set
var text1 = -1L
    private set
var toolTipContentView = -1L
    private set
var topEnd = -1L
    private set
var topStart = -1L
    private set
var topBarMenuItemImageView = -1L
    private set
var tosFooter = -1L
    private set
var touchOutside = -1L
    private set
var trimSilenceSwitch = -1L
    private set
var varispeedUnavailableTitle = -1L
    private set
var ytmLogo = -1L
    private set
var ytmLogoRingo2 = -1L
    private set

internal val sharedResourceIdPatch = resourcePatch(
    description = "sharedResourceIdPatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        accountSwitcherAccessibility = resourceMappings[
            STRING,
            "account_switcher_accessibility_label",
        ]
        actionBarLogo = resourceMappings[
            DRAWABLE,
            "action_bar_logo",
        ]
        actionBarLogoRingo2 = resourceMappings[
            DRAWABLE,
            "action_bar_logo_ringo2",
        ]
        bottomSheetRecyclerView = resourceMappings[
            LAYOUT,
            "bottom_sheet_recycler_view"
        ]
        buttonContainer = resourceMappings[
            ID,
            "button_container"
        ]
        buttonIconPaddingMedium = resourceMappings[
            DIMEN,
            "button_icon_padding_medium"
        ]
        chipCloud = resourceMappings[
            LAYOUT,
            "chip_cloud"
        ]
        colorGrey = resourceMappings[
            COLOR,
            "ytm_color_grey_12"
        ]
        darkBackground = resourceMappings[
            ID,
            "dark_background"
        ]
        designBottomSheetDialog = resourceMappings[
            LAYOUT,
            "design_bottom_sheet_dialog"
        ]
        endButtonsContainer = resourceMappings[
            ID,
            "end_buttons_container"
        ]
        floatingLayout = resourceMappings[
            ID,
            "floating_layout"
        ]
        historyMenuItem = resourceMappings[
            ID,
            "history_menu_item"
        ]
        inlineTimeBarAdBreakMarkerColor = resourceMappings[
            COLOR,
            "inline_time_bar_ad_break_marker_color"
        ]
        interstitialsContainer = resourceMappings[
            ID,
            "interstitials_container"
        ]
        isTablet = resourceMappings[
            BOOL,
            "is_tablet"
        ]
        likeDislikeContainer = resourceMappings[
            ID,
            "like_dislike_container"
        ]
        mainActivityLaunchAnimation = resourceMappings[
            LAYOUT,
            "main_activity_launch_animation"
        ]
        menuEntry = resourceMappings[
            LAYOUT,
            "menu_entry"
        ]
        miniPlayerDefaultText = resourceMappings[
            STRING,
            "mini_player_default_text"
        ]
        miniPlayerMdxPlaying = resourceMappings[
            STRING,
            "mini_player_mdx_playing"
        ]
        miniPlayerPlayPauseReplayButton = resourceMappings[
            ID,
            "mini_player_play_pause_replay_button"
        ]
        miniPlayerViewPager = resourceMappings[
            ID,
            "mini_player_view_pager"
        ]
        musicNotifierShelf = resourceMappings[
            LAYOUT,
            "music_notifier_shelf"
        ]
        musicTasteBuilderShelf = resourceMappings[
            LAYOUT,
            "music_tastebuilder_shelf"
        ]
        namesInactiveAccountThumbnailSize = resourceMappings[
            DIMEN,
            "names_inactive_account_thumbnail_size"
        ]
        offlineSettingsMenuItem = resourceMappings[
            ID,
            "offline_settings_menu_item"
        ]
        playerOverlayChip = resourceMappings[
            ID,
            "player_overlay_chip"
        ]
        playerViewPager = resourceMappings[
            ID,
            "player_view_pager"
        ]
        privacyTosFooter = resourceMappings[
            ID,
            "privacy_tos_footer"
        ]
        qualityAuto = resourceMappings[
            STRING,
            "quality_auto"
        ]
        remixGenericButtonSize = resourceMappings[
            DIMEN,
            "remix_generic_button_size"
        ]
        slidingDialogAnimation = resourceMappings[
            STYLE,
            "SlidingDialogAnimation"
        ]
        tapBloomView = resourceMappings[
            ID,
            "tap_bloom_view"
        ]
        text1 = resourceMappings[
            ID,
            "text1"
        ]
        toolTipContentView = resourceMappings[
            LAYOUT,
            "tooltip_content_view"
        ]
        topEnd = resourceMappings[
            ID,
            "TOP_END"
        ]
        topStart = resourceMappings[
            ID,
            "TOP_START"
        ]
        topBarMenuItemImageView = resourceMappings[
            ID,
            "top_bar_menu_item_image_view"
        ]
        tosFooter = resourceMappings[
            ID,
            "tos_footer"
        ]
        touchOutside = resourceMappings[
            ID,
            "touch_outside"
        ]
        trimSilenceSwitch = resourceMappings[
            ID,
            "trim_silence_switch"
        ]
        varispeedUnavailableTitle = resourceMappings[
            STRING,
            "varispeed_unavailable_title"
        ]
        ytmLogo = resourceMappings[
            DRAWABLE,
            "ytm_logo",
        ]
        ytmLogoRingo2 = resourceMappings[
            DRAWABLE,
            "ytm_logo_ringo2",
        ]
    }
}