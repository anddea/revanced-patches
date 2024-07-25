package app.revanced.patches.youtube.layout.playerbuttonbg

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

@Suppress("Deprecation", "unused")
object PlayerButtonBackgroundPatch : BaseResourcePatch(
    name = "Force player buttons background",
    description = "Changes, at compile time, the dark background surrounding the video player controls.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
) {
    private const val BACKGROUND = "?ytOverlayBackgroundMediumLight"

    private val BackgroundColor by stringPatchOption(
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

    override fun execute(context: ResourceContext) {

        context.xmlEditor["res/drawable/player_button_circle_background.xml"].use { editor ->
            editor.file.doRecursively { node ->
                arrayOf("color").forEach replacement@{ replacement ->
                    if (node !is Element) return@replacement

                    node.getAttributeNode("android:$replacement")?.let { attribute ->
                        attribute.textContent = BackgroundColor
                    }
                }
            }
        }

        SettingsPatch.updatePatchStatus(this)
    }
}
