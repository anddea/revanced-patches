package app.morphe.patches.youtube.layout.playerbuttonbg

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.doRecursively
import org.w3c.dom.Element

private const val BACKGROUND = "?ytOverlayBackgroundMediumLight"

@Suppress("unused")
val playerButtonBackgroundPatch = resourcePatch(
    FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND.title,
    FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val backgroundColor by stringOption(
        key = "BackgroundColor",
        default = BACKGROUND,
        values = mapOf(
            "Default" to BACKGROUND,
            "Transparent" to "@android:color/transparent",
            "Opacity10" to "#1a000000",
            "Opacity20" to "#33000000",
            "Opacity30" to "#4d000000",
            "Opacity40" to "#66000000",
            "Opacity50" to "#80000000",
            "Opacity60" to "#99000000",
            "Opacity70" to "#b3000000",
            "Opacity80" to "#cc000000",
            "Opacity90" to "#e6000000",
            "Opacity100" to "#ff000000",
        ),
        title = "Background color",
        description = "Specify a background color for player buttons using a hex color code. The first two symbols of the hex code represent the alpha channel, which is used to change the opacity."
    )

    execute {
        document("res/drawable/player_button_circle_background.xml").use { document ->

            document.doRecursively node@{ node ->
                if (node !is Element) return@node

                node.getAttributeNode("android:color")?.let { attribute ->
                    attribute.textContent = backgroundColor
                }
            }
        }

        addPreference(FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND)

    }
}
