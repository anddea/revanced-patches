package app.revanced.patches.music.general.amoled.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.shared.patch.litho.LithoThemePatch
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH
import org.w3c.dom.Element

@Patch
@Name("Amoled")
@Description("Applies pure black theme on some components.")
@DependsOn([LithoThemePatch::class])
@MusicCompatibility
class AmoledPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        LithoThemePatch.injectCall("$MUSIC_UTILS_PATH/LithoThemePatch;->applyLithoTheme(I)I")

        context.xmlEditor["res/values/colors.xml"].use { editor ->
            val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

            for (i in 0 until resourcesNode.childNodes.length) {
                val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                node.textContent = when (node.getAttribute("name")) {
                    "yt_black0", "yt_black1", "yt_black1_opacity95", "yt_black1_opacity98", "yt_black2", "yt_black3",
                    "yt_black4", "yt_status_bar_background_dark", "ytm_color_grey_12", "material_grey_850" -> "@android:color/black"

                    else -> continue
                }
            }
        }

    }
}
