package app.revanced.patches.youtube.layout.shortcut

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.findElementByAttributeValueOrThrow
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

@Suppress("DEPRECATION", "unused")
object ShortcutPatch : BaseResourcePatch(
    name = "Hide shortcuts",
    description = "Remove, at compile time, the app shortcuts that appears when app icon is long pressed.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    private val Explore by booleanPatchOption(
        key = "Explore",
        default = false,
        title = "Hide Explore",
        description = "Hide Explore from shortcuts.",
        required = true
    )

    private val Subscriptions by booleanPatchOption(
        key = "Subscriptions",
        default = false,
        title = "Hide Subscriptions",
        description = "Hide Subscriptions from shortcuts.",
        required = true
    )

    private val Search by booleanPatchOption(
        key = "Search",
        default = false,
        title = "Hide Search",
        description = "Hide Search from shortcuts.",
        required = true
    )

    private val Shorts by booleanPatchOption(
        key = "Shorts",
        default = true,
        title = "Hide Shorts",
        description = "Hide Shorts from shortcuts.",
        required = true
    )

    override fun execute(context: ResourceContext) {

        this.options.values.forEach { options ->
            if (options.value == true) {
                context.xmlEditor["res/xml/main_shortcuts.xml"].use { editor ->
                    val shortcuts = editor.file.getElementsByTagName("shortcuts").item(0) as Element
                    val shortsItem = shortcuts.getElementsByTagName("shortcut")
                        .findElementByAttributeValueOrThrow(
                            "android:shortcutId",
                            "${options.key.lowercase()}-shortcut"
                        )
                    shortsItem.parentNode.removeChild(shortsItem)
                }
            }
        }

        SettingsPatch.updatePatchStatus(this)
    }
}