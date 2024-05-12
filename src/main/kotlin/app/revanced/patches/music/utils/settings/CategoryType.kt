package app.revanced.patches.music.utils.settings

enum class CategoryType(val value: String, var added: Boolean) {
    ACCOUNT("account", false),
    ACTION_BAR("action_bar", false),
    ADS("ads", false),
    FLYOUT("flyout", false),
    GENERAL("general", false),
    NAVIGATION("navigation", false),
    PLAYER("player", false),
    VIDEO("video", false),
    RETURN_YOUTUBE_DISLIKE("ryd", false),
    SPONSOR_BLOCK("sb", false),
    MISC("misc", false)
}