package app.revanced.patches.youtube.layout.forcebackground.patch

import app.revanced.extensions.doRecursively
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import org.w3c.dom.Element

@Patch(false)
@Name("Force hide player button background")
@Description("Force hides the background from the video player buttons.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class PlayerButtonBackgroundPatch : ResourcePatch {

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