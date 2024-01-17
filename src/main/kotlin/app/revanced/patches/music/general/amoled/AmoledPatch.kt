package app.revanced.patches.music.general.amoled

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.shared.patch.litho.LithoThemePatch
import org.w3c.dom.Element

@Patch(
    name = "Amoled",
    description = "Applies a pure black theme to some components.",
    dependencies = [LithoThemePatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ],
)
@Suppress("unused")
object AmoledPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        LithoThemePatch.injectCall("$UTILS_PATH/LithoThemePatch;->applyLithoTheme(I)I")

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
