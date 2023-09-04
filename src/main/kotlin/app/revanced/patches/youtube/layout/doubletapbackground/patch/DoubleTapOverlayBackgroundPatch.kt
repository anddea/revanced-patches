package app.revanced.patches.youtube.layout.doubletapbackground.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch(false)
@Name("Hide double tap overlay filter")
@Description("Hides the double tap dark filter layer.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class DoubleTapOverlayBackgroundPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {
        context.xmlEditor[RESOURCE_FILE_PATH].use {
            it.file.getElementsByTagName("merge").item(0).childNodes.apply {
                val attributes = arrayOf("height", "width")
                for (i in 1 until length) {
                    val view = item(i)
                    if (
                        view.hasAttributes() &&
                        view.attributes.getNamedItem("android:id").nodeValue.endsWith("tap_bloom_view")
                    ) {
                        attributes.forEach { attribute ->
                            view.attributes.getNamedItem("android:layout_$attribute").nodeValue =
                                "0.0dip"
                        }
                    }
                    if (
                        view.hasAttributes() &&
                        view.attributes.getNamedItem("android:id").nodeValue.endsWith("dark_background")
                    ) {
                        view.attributes.getNamedItem("android:src").nodeValue =
                            "@color/full_transparent"
                        break
                    }
                }
            }
        }

        SettingsPatch.updatePatchStatus("hide-double-tap-overlay-filter")

    }

    private companion object {
        const val RESOURCE_FILE_PATH = "res/layout/quick_seek_overlay.xml"
    }
}