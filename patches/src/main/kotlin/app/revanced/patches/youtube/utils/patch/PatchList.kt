package app.revanced.patches.youtube.utils.patch

internal enum class PatchList(
    val title: String,
    val summary: String,
    var included: Boolean? = false
) {
    ALTERNATIVE_THUMBNAILS(
        "Alternative thumbnails",
        "Adds options to replace video thumbnails using the DeArrow API or image captures from the video."
    ),
    AMBIENT_MODE_CONTROL(
        "Ambient mode control",
        "Adds options to disable Ambient mode and to bypass Ambient mode restrictions."
    ),
    BYPASS_IMAGE_REGION_RESTRICTIONS(
        "Bypass image region restrictions",
        "Adds an option to use a different host for static images, so that images blocked in some countries can be received."
    ),
    CHANGE_PLAYER_FLYOUT_MENU_TOGGLES(
        "Change player flyout menu toggles",
        "Adds an option to use text toggles instead of switch toggles within the additional settings menu."
    ),
    CHANGE_SHARE_SHEET(
        "Change share sheet",
        "Add option to change from in-app share sheet to system share sheet."
    ),
    CHANGE_START_PAGE(
        "Change start page",
        "Adds an option to set which page the app opens in instead of the homepage."
    ),
    CUSTOM_SHORTS_ACTION_BUTTONS(
        "Custom Shorts action buttons",
        "Changes, at compile time, the icon of the action buttons of the Shorts player."
    ),
    CUSTOM_BRANDING_ICON_FOR_YOUTUBE(
        "Custom branding icon for YouTube",
        "Changes the YouTube app icon to the icon specified in patch options."
    ),
    CUSTOM_BRANDING_NAME_FOR_YOUTUBE(
        "Custom branding name for YouTube",
        "Renames the YouTube app to the name specified in patch options."
    ),
    CUSTOM_DOUBLE_TAP_LENGTH(
        "Custom double tap length",
        "Adds Double-tap to seek values that are specified in patch options."
    ),
    CUSTOM_HEADER_FOR_YOUTUBE(
        "Custom header for YouTube",
        "Applies a custom header in the top left corner within the app."
    ),
    DESCRIPTION_COMPONENTS(
        "Description components",
        "Adds options to hide and disable description components."
    ),
    DISABLE_QUIC_PROTOCOL(
        "Disable QUIC protocol",
        "Adds an option to disable CronetEngine's QUIC protocol."
    ),
    DISABLE_AUTO_AUDIO_TRACKS(
        "Disable auto audio tracks",
        "Adds an option to disable audio tracks from being automatically enabled."
    ),
    DISABLE_AUTO_CAPTIONS(
        "Disable auto captions",
        "Adds an option to disable captions from being automatically enabled."
    ),
    DISABLE_HAPTIC_FEEDBACK(
        "Disable haptic feedback",
        "Adds options to disable haptic feedback when swiping in the video player."
    ),
    DISABLE_RESUMING_SHORTS_ON_STARTUP(
        "Disable resuming Shorts on startup",
        "Adds an option to disable the Shorts player from resuming on app startup when Shorts were last being watched."
    ),
    DISABLE_SPLASH_ANIMATION(
        "Disable splash animation",
        "Adds an option to disable the splash animation on app startup."
    ),
    ENABLE_OPUS_CODEC(
        "Enable OPUS codec",
        "Adds an options to enable the OPUS audio codec if the player response includes."
    ),
    ENABLE_DEBUG_LOGGING(
        "Enable debug logging",
        "Adds an option to enable debug logging."
    ),
    ENABLE_EXTERNAL_BROWSER(
        "Enable external browser",
        "Adds an option to always open links in your browser instead of in the in-app-browser."
    ),
    ENABLE_GRADIENT_LOADING_SCREEN(
        "Enable gradient loading screen",
        "Adds an option to enable the gradient loading screen."
    ),
    ENABLE_OPEN_LINKS_DIRECTLY(
        "Enable open links directly",
        "Adds an option to skip over redirection URLs in external links."
    ),
    FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND(
        "Force player buttons background",
        "Changes the dark background surrounding the video player controls at compile time."
    ),
    FORCE_SNACKBAR_THEME(
        "Force snackbar theme",
        "Changes snackbar background color to match selected theme at compile time."
    ),
    FULLSCREEN_COMPONENTS(
        "Fullscreen components",
        "Adds options to hide or change components related to fullscreen."
    ),
    GMSCORE_SUPPORT(
        "GmsCore support",
        "Allows patched Google apps to run without root and under a different package name by using GmsCore instead of Google Play Services."
    ),
    HIDE_SHORTS_DIMMING(
        "Hide Shorts dimming",
        "Removes, at compile time, the dimming effect at the top and bottom of Shorts videos."
    ),
    HIDE_ACTION_BUTTONS(
        "Hide action buttons",
        "Adds options to hide action buttons under videos."
    ),
    HIDE_ADS(
        "Hide ads",
        "Adds options to hide ads."
    ),
    HIDE_COMMENTS_COMPONENTS(
        "Hide comments components",
        "Adds options to hide components related to comments."
    ),
    HIDE_FEED_COMPONENTS(
        "Hide feed components",
        "Adds options to hide components related to feeds."
    ),
    HIDE_FEED_FLYOUT_MENU(
        "Hide feed flyout menu",
        "Adds the ability to hide feed flyout menu components using a custom filter."
    ),
    HIDE_LAYOUT_COMPONENTS(
        "Hide layout components",
        "Adds options to hide general layout components."
    ),
    HIDE_PLAYER_BUTTONS(
        "Hide player buttons",
        "Adds options to hide buttons in the video player."
    ),
    HIDE_PLAYER_FLYOUT_MENU(
        "Hide player flyout menu",
        "Adds options to hide player flyout menu components."
    ),
    HIDE_SHORTCUTS(
        "Hide shortcuts",
        "Remove, at compile time, the app shortcuts that appears when app icon is long pressed."
    ),
    HOOK_YOUTUBE_MUSIC_ACTIONS(
        "Hook YouTube Music actions",
        "Adds support for opening music in RVX Music using the in-app YouTube Music button."
    ),
    HOOK_DOWNLOAD_ACTIONS(
        "Hook download actions",
        "Adds support to download videos with an external downloader app using the in-app download button."
    ),
    LAYOUT_SWITCH(
        "Layout switch",
        "Adds an option to spoof the dpi in order to use a tablet or phone layout."
    ),
    MATERIALYOU(
        "MaterialYou",
        "Applies the MaterialYou theme for Android 12+ devices."
    ),
    MINIPLAYER(
        "Miniplayer",
        "Adds options to change the in app minimized player, and if patching target 19.16+ adds options to use modern miniplayers."
    ),
    NAVIGATION_BAR_COMPONENTS(
        "Navigation bar components",
        "Adds options to hide or change components related to the navigation bar."
    ),
    OVERLAY_BUTTONS(
        "Overlay buttons",
        "Adds options to display overlay buttons in the video player."
    ),
    PLAYER_COMPONENTS(
        "Player components",
        "Adds options to hide or change components related to the video player."
    ),
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS(
        "Remove background playback restrictions",
        "Removes restrictions on background playback, including for music and kids videos."
    ),
    REMOVE_VIEWER_DISCRETION_DIALOG(
        "Remove viewer discretion dialog",
        "Adds an option to remove the dialog that appears when opening a video that has been age-restricted by accepting it automatically. This does not bypass the age restriction."
    ),
    RETURN_YOUTUBE_DISLIKE(
        "Return YouTube Dislike",
        "Adds an option to show the dislike count of videos using the Return YouTube Dislike API."
    ),
    RETURN_YOUTUBE_USERNAME(
        "Return YouTube Username",
        "Adds an option to replace YouTube handles with usernames in comments using YouTube Data API v3."
    ),
    SANITIZE_SHARING_LINKS(
        "Sanitize sharing links",
        "Adds an option to remove tracking query parameters from URLs when sharing links."
    ),
    SEEKBAR_COMPONENTS(
        "Seekbar components",
        "Adds options to hide or change components related to the seekbar."
    ),
    SETTINGS_FOR_YOUTUBE(
        "Settings for YouTube",
        "Applies mandatory patches to implement ReVanced Extended settings into the application."
    ),
    SHORTS_COMPONENTS(
        "Shorts components",
        "Adds options to hide or change components related to YouTube Shorts."
    ),
    SPONSORBLOCK(
        "SponsorBlock",
        "Adds options to enable and configure SponsorBlock, which can skip undesired video segments, such as sponsored content."
    ),
    SPOOF_APP_VERSION(
        "Spoof app version",
        "Adds options to spoof the YouTube client version. This can be used to restore old UI elements and features."
    ),
    SPOOF_STREAMING_DATA(
        "Spoof streaming data",
        "Adds options to spoof the streaming data to allow video playback."
    ),
    SWIPE_CONTROLS(
        "Swipe controls",
        "Adds options for controlling volume and brightness with swiping, and whether to enter fullscreen when swiping down below the player."
    ),
    THEME(
        "Theme",
        "Changes the app's theme to the values specified in patch options."
    ),
    TOOLBAR_COMPONENTS(
        "Toolbar components",
        "Adds options to hide or change components located on the toolbar, such as toolbar buttons, search bar, and header."
    ),
    TRANSLATIONS_FOR_YOUTUBE(
        "Translations for YouTube",
        "Add translations or remove string resources."
    ),
    VIDEO_PLAYBACK(
        "Video playback",
        "Adds options to customize settings related to video playback, such as default video quality and playback speed."
    ),
    VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE(
        "Visual preferences icons for YouTube",
        "Adds icons to specific preferences in the settings."
    ),
    WATCH_HISTORY(
        "Spoof watch history",
        "Adds an option to change the domain of the watch history or check its status."
    )
}