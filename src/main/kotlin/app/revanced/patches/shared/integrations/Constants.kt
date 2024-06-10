package app.revanced.patches.shared.integrations

@Suppress("MemberVisibilityCanBePrivate")
object Constants {
    const val INTEGRATIONS_PATH = "Lapp/revanced/integrations/shared"
    const val PATCHES_PATH = "$INTEGRATIONS_PATH/patches"
    const val COMPONENTS_PATH = "$PATCHES_PATH/components"

    const val INTEGRATIONS_SETTING_CLASS_DESCRIPTOR = "$INTEGRATIONS_PATH/settings/Setting;"
    const val INTEGRATIONS_UTILS_CLASS_DESCRIPTOR = "$INTEGRATIONS_PATH/utils/Utils;"
}