package app.revanced.patches.youtube.layout.playerbuttonbg

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

@Suppress("Deprecation", "unused")
object PlayerButtonBackgroundPatch : BaseResourcePatch(
    name = "Force hide player buttons background",
    description = "Removes, at compile time, the dark background surrounding the video player controls.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
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

        SettingsPatch.updatePatchStatus(this)
    }
}
