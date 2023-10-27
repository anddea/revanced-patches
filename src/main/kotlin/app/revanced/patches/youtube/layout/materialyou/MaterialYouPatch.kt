package app.revanced.patches.youtube.layout.materialyou

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.layout.theme.GeneralThemePatch
import app.revanced.patches.youtube.layout.theme.GeneralThemePatch.isMonetPatchIncluded
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.resources.ResourceHelper.updatePatchStatusTheme
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import app.revanced.util.resources.ResourceUtils.copyXmlNode

@Patch(
    name = "MaterialYou",
    description = "Enables MaterialYou theme for Android 12+",
    dependencies = [
        GeneralThemePatch::class,
        SettingsPatch::class
    ],
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
                "18.39.41",
                "18.40.34",
                "18.41.39"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object MaterialYouPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        arrayOf(
            ResourceUtils.ResourceGroup(
                "drawable-night-v31",
                "new_content_dot_background.xml"
            ),
            ResourceUtils.ResourceGroup(
                "drawable-v31",
                "new_content_count_background.xml",
                "new_content_dot_background.xml"
            ),
            ResourceUtils.ResourceGroup(
                "layout-v31",
                "new_content_count.xml"
            )
        ).forEach {
            context["res/${it.resourceDirectoryName}"].mkdirs()
            context.copyResources("youtube/materialyou", it)
        }

        context.copyXmlNode("youtube/materialyou/host", "values-v31/colors.xml", "resources")

        /**
         * Add settings
         */
        context.updatePatchStatusTheme("materialyou")

        isMonetPatchIncluded = true

    }
}
