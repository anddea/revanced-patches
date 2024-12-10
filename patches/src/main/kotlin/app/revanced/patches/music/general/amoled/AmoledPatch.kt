package app.revanced.patches.music.general.amoled

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.music.utils.patch.PatchList.AMOLED
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.drawable.addDrawableColorHook
import app.revanced.patches.shared.drawable.drawableColorHookPatch
import org.w3c.dom.Element

@Suppress("unused")
val amoledPatch = resourcePatch(
    AMOLED.title,
    AMOLED.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        drawableColorHookPatch,
        settingsPatch
    )

    execute {
        addDrawableColorHook("$UTILS_PATH/DrawableColorPatch;->getColor(I)I")

        document("res/values/colors.xml").use { document ->
            val resourcesNode = document.getElementsByTagName("resources").item(0) as Element

            for (i in 0 until resourcesNode.childNodes.length) {
                val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                node.textContent = when (node.getAttribute("name")) {
                    "yt_black0", "yt_black1", "yt_black1_opacity95", "yt_black1_opacity98", "yt_black2", "yt_black3",
                    "yt_black4", "yt_status_bar_background_dark", "ytm_color_grey_12", "material_grey_850" -> "@android:color/black"

                    else -> continue
                }
            }
        }

        updatePatchStatus(AMOLED)

    }
}
