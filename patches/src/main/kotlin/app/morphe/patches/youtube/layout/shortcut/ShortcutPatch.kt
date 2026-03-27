package app.morphe.patches.youtube.layout.shortcut

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_SHORTCUTS
import app.morphe.patches.youtube.utils.playservice.is_19_44_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findElementByAttributeValueOrThrow
import org.w3c.dom.Element

@Suppress("unused")
val shortcutPatch = resourcePatch(
    HIDE_SHORTCUTS.title,
    HIDE_SHORTCUTS.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch
    )

    val explore = booleanOption(
        key = "explore",
        default = false,
        title = "Hide Explore",
        description = "Hide Explore from shortcuts.",
        required = true
    )

    val subscriptions = booleanOption(
        key = "subscriptions",
        default = false,
        title = "Hide Subscriptions",
        description = "Hide Subscriptions from shortcuts.",
        required = true
    )

    val search = booleanOption(
        key = "search",
        default = false,
        title = "Hide Search",
        description = "Hide Search from shortcuts.",
        required = true
    )

    val shorts = booleanOption(
        key = "shorts",
        default = true,
        title = "Hide Shorts",
        description = "Hide Shorts from shortcuts.",
        required = true
    )

    execute {
        var options = listOf(
            subscriptions,
            search,
            shorts
        )

        if (!is_19_44_or_greater) {
            options += explore
        }

        options.forEach { option ->
            if (option.value == true) {
                document("res/xml/main_shortcuts.xml").use { document ->
                    val shortcuts = document.getElementsByTagName("shortcuts").item(0) as Element
                    val shortsItem = shortcuts.getElementsByTagName("shortcut")
                        .findElementByAttributeValueOrThrow(
                            "android:shortcutId",
                            "${option.key}-shortcut"
                        )
                    shortsItem.parentNode.removeChild(shortsItem)
                }
            }
        }

        addPreference(HIDE_SHORTCUTS)

    }
}
