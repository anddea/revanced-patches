package app.revanced.patches.reddit.utils.patch

internal enum class PatchList(
    val title: String,
    val summary: String,
    var included: Boolean? = false
) {
    CHANGE_PACKAGE_NAME(
        "Change package name",
        "Changes the package name for Reddit to the name specified in patch options."
    ),
    CUSTOM_BRANDING_NAME_FOR_REDDIT(
        "Custom branding name for Reddit",
        "Renames the Reddit app to the name specified in patch options."
    ),
    DISABLE_SCREENSHOT_POPUP(
        "Disable screenshot popup",
        "Adds an option to disable the popup that appears when taking a screenshot."
    ),
    HIDE_RECENTLY_VISITED_SHELF(
        "Hide Recently Visited shelf",
        "Adds an option to hide the Recently Visited shelf in the sidebar."
    ),
    HIDE_ADS(
        "Hide ads",
        "Adds options to hide ads."
    ),
    HIDE_NAVIGATION_BUTTONS(
        "Hide navigation buttons",
        "Adds options to hide buttons in the navigation bar."
    ),
    HIDE_RECOMMENDED_COMMUNITIES_SHELF(
        "Hide recommended communities shelf",
        "Adds an option to hide the recommended communities shelves in subreddits."
    ),
    HIDE_TOOLBAR_BUTTON(
        "Hide toolbar button",
        "Adds an option to hide the r/place or Reddit recap button in the toolbar."
    ),
    OPEN_LINKS_DIRECTLY(
        "Open links directly",
        "Adds an option to skip over redirection URLs in external links."
    ),
    OPEN_LINKS_EXTERNALLY(
        "Open links externally",
        "Adds an option to always open links in your browser instead of in the in-app-browser."
    ),
    PREMIUM_ICON(
        "Premium icon",
        "Unlocks premium app icons."
    ),
    REMOVE_SUBREDDIT_DIALOG(
        "Remove subreddit dialog",
        "Adds options to remove the NSFW community warning and notifications suggestion dialogs by dismissing them automatically."
    ),
    SANITIZE_SHARING_LINKS(
        "Sanitize sharing links",
        "Adds an option to remove tracking query parameters from URLs when sharing links."
    ),
    SETTINGS_FOR_REDDIT(
        "Settings for Reddit",
        "Applies mandatory patches to implement ReVanced Extended settings into the application."
    )
}