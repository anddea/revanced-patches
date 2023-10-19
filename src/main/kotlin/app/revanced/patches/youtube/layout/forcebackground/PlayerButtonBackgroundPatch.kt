package app.revanced.patches.youtube.layout.forcebackground

import app.revanced.extensions.doRecursively
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import org.w3c.dom.Element

@Patch(
    name = "Force hide player button background",
    description = "Force hides the background from the video player buttons.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object PlayerButtonBackgroundPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {
        context.xmlEditor["res/drawable/player_button_circle_background.xml"].use { editor ->
            editor.file.doRecursively { node ->
                arrayOf("color").forEach replacement@{ replacement ->
                    if (node !is Element) return@replacement

                    node.getAttributeNode("android:$replacement")?.let { attribute ->
                        attribute.textContent = "@android:color/transparent"
                    }
                }
            }
        }

        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText()
                .replace(
                    "HIDE_PLAYER_BUTTON_BACKGROUND",
                    "FORCE_BUTTON_BACKGROUND"
                )
        )

    }
}