package app.revanced.patches.youtube.player.fullscreenbutton

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import org.w3c.dom.Element

@Patch(
    name = "Hide fullscreen button",
    description = "Force to hide fullscreen button in player bottom UI container.",
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
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object HideFullscreenButtonPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        context.xmlEditor["res/layout/youtube_controls_bottom_ui_container.xml"].use { editor ->
            editor.file.doRecursively { node ->
                if (node is Element && (
                    node.getAttribute("android:id") == "@id/fullscreen_button" ||
                    node.getAttribute("android:id") == "@id/youtube_controls_fullscreen_button_stub")
                ) {
                    node.apply {
                        setAttribute("android:layout_height", "0.0dip")
                        setAttribute("android:layout_width", "0.0dip")
                    }
                }
            }
        }

        // For newer versions of YouTube (19.09.xx+), there's a new layout file for fullscreen button
        try {
            context.xmlEditor["res/layout/youtube_controls_fullscreen_button.xml"].use { editor ->
                editor.file.doRecursively { node ->
                    if (node is Element && node.getAttribute("android:id") == "@id/fullscreen_button") {
                        node.apply {
                            setAttribute("android:layout_height", "0.0dip")
                            setAttribute("android:layout_width", "0.0dip")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Do nothing
        }

        SettingsPatch.updatePatchStatus("Hide fullscreen button")

    }
}