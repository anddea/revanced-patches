package app.revanced.util.enum

internal enum class CategoryType(val value: String, var added: Boolean) {
    ADS("ads", false),
    BUTTON_CONTAINER("button_container", false),
    FLYOUT("flyout", false),
    GENERAL("general", false),
    MISC("misc", false),
    NAVIGATION("navigation", false),
    PLAYER("player", false)
}