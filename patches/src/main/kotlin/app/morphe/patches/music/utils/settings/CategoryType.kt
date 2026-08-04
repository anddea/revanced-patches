package app.morphe.patches.music.utils.settings

internal enum class CategoryType(val value: String, var added: Boolean) {
    GENERAL("general", false),
    ACCOUNT("account", false),
    ACTION_BAR("action_bar", false),
    ADS("ads", false),
    FLYOUT("flyout", false),
    LYRICS("lyrics", false),
    NAVIGATION("navigation", false),
    PLAYER("player", false),
    VIDEO("video", false),
    RETURN_YOUTUBE_DISLIKE("ryd", false),
    RETURN_YOUTUBE_USERNAME("return_youtube_username", false),
    SPONSOR_BLOCK("sb", false),
    MISC("misc", false)
}