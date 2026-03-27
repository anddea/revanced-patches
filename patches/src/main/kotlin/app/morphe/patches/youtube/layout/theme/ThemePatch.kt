package app.morphe.patches.youtube.layout.theme

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.MATERIALYOU
import app.morphe.patches.youtube.utils.patch.PatchList.THEME
import app.morphe.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusTheme
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.valueOrThrow
import org.w3c.dom.Element

@Suppress("unused")
val themePatch = resourcePatch(
    THEME.title,
    THEME.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedThemePatch,
        settingsPatch,
    )

    val amoledBlackColor = "@android:color/black"
    val whiteColor = "@android:color/white"

    val availableDarkTheme = mapOf(
        "Amoled Black" to amoledBlackColor,
        "Classic (Old YouTube)" to "#FF212121",
        "Catppuccin (Mocha)" to "#FF181825",
        "Dark Pink" to "#FF290025",
        "Dark Blue" to "#FF001029",
        "Dark Green" to "#FF002905",
        "Dark Yellow" to "#FF282900",
        "Dark Orange" to "#FF291800",
        "Dark Red" to "#FF290000",
    )

    val availableLightTheme = mapOf(
        "White" to whiteColor,
        "Catppuccin (Latte)" to "#FFE6E9EF",
        "Light Pink" to "#FFFCCFF3",
        "Light Blue" to "#FFD1E0FF",
        "Light Green" to "#FFCCFFCC",
        "Light Yellow" to "#FFFDFFCC",
        "Light Orange" to "#FFFFE6CC",
        "Light Red" to "#FFFFD6D6",
        "Pale Blue" to "#FFD4FFF8",
        "Pale Green" to "#FFD1FFCC",
        "Pale Yellow" to "#FFFFE9AA",
    )

    val darkThemeBackgroundColor = stringOption(
        key = "darkThemeBackgroundColor",
        default = amoledBlackColor,
        values = availableDarkTheme,
        title = "Dark theme background color",
        description = "Can be a hex color (#AARRGGBB) or a color resource reference.",
        required = true,
    )

    val lightThemeBackgroundColor = stringOption(
        key = "lightThemeBackgroundColor",
        default = whiteColor,
        values = availableLightTheme,
        title = "Light theme background color",
        description = "Can be a hex color (#AARRGGBB) or a color resource reference.",
        required = true,
    )

    execute {

        // Check patch options first.
        val darkThemeColor = darkThemeBackgroundColor
            .valueOrThrow()

        val lightThemeColor = lightThemeBackgroundColor
            .valueOrThrow()

        arrayOf("values", "values-v31").forEach { path ->
            document("res/$path/colors.xml").use { document ->
                val resourcesNode = document.documentElement
                val childNodes = resourcesNode.childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue

                    node.textContent = when (node.getAttribute("name")) {
                        "yt_black0", "yt_black1", "yt_black1_opacity95", "yt_black1_opacity98", "yt_black2", "yt_black3",
                        "yt_black4", "yt_status_bar_background_dark", "material_grey_850" -> darkThemeColor

                        else -> continue
                    }
                }
            }
        }

        document("res/values/colors.xml").use { document ->
            val resourcesNode = document.getElementsByTagName("resources").item(0) as Element

            val children = resourcesNode.childNodes
            for (i in 0 until children.length) {
                val node = children.item(i) as? Element ?: continue

                node.textContent = when (node.getAttribute("name")) {
                    "yt_white1", "yt_white1_opacity95", "yt_white1_opacity98",
                    "yt_white2", "yt_white3", "yt_white4",
                        -> lightThemeColor

                    else -> continue
                }
            }
        }

        var darkThemeString = "Custom"
        var lightThemeString = "Custom"
        availableDarkTheme.forEach { (k, v) ->
            if (v == darkThemeColor) darkThemeString = k
        }
        availableLightTheme.forEach { (k, v) ->
            if (v == lightThemeColor) lightThemeString = k
        }
        val themeString = if (lightThemeColor != whiteColor)
            "$lightThemeString + $darkThemeString"
        else
            darkThemeString
        val currentTheme = if (MATERIALYOU.included == true)
            "MaterialYou + $themeString"
        else
            themeString

        updatePatchStatusTheme(currentTheme)

    }
}
