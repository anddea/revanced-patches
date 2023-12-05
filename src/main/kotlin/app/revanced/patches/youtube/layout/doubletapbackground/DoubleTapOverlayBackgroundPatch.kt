package app.revanced.patches.youtube.layout.doubletapbackground

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.settings.SettingsPatch

@Patch(
    name = "Hide double tap overlay filter",
    description = "Hides the double tap dark filter layer.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
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
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object DoubleTapOverlayBackgroundPatch : ResourcePatch() {
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

        SettingsPatch.updatePatchStatus("Hide double tap overlay filter")

    }

    private const val RESOURCE_FILE_PATH = "res/layout/quick_seek_overlay.xml"
}