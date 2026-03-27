package app.morphe.patches.shared.extension

@Suppress("MemberVisibilityCanBePrivate")
internal object Constants {
    const val EXTENSION_PATH = "Lapp/morphe/extension/shared"
    const val PATCHES_PATH = "$EXTENSION_PATH/patches"
    const val COMPONENTS_PATH = "$PATCHES_PATH/components"
    const val SPANS_PATH = "$PATCHES_PATH/spans"
    const val SPOOF_PATH = "$PATCHES_PATH/spoof"

    const val EXTENSION_UTILS_PATH = "$EXTENSION_PATH/utils"
    const val EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR = "$PATCHES_PATH/PatchStatus;"
    const val EXTENSION_SETTING_CLASS_DESCRIPTOR = "$EXTENSION_PATH/settings/Setting;"
    const val EXTENSION_UTILS_CLASS_DESCRIPTOR = "$EXTENSION_UTILS_PATH/Utils;"
    const val EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR =
        "$EXTENSION_UTILS_PATH/BaseThemeUtils;"
}