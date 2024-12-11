package app.revanced.patches.youtube.layout.theme

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.MATERIALYOU
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusTheme
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode

@Suppress("unused")
val materialYouPatch = resourcePatch(
    MATERIALYOU.title,
    MATERIALYOU.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedThemePatch,
        settingsPatch,
    )

    execute {
        arrayOf(
            ResourceGroup(
                "drawable-night-v31",
                "new_content_dot_background.xml"
            ),
            ResourceGroup(
                "drawable-v31",
                "new_content_count_background.xml",
                "new_content_dot_background.xml"
            ),
            ResourceGroup(
                "layout-v31",
                "new_content_count.xml"
            )
        ).forEach {
            get("res/${it.resourceDirectoryName}").mkdirs()
            copyResources("youtube/materialyou", it)
        }

        copyXmlNode("youtube/materialyou/host", "values-v31/colors.xml", "resources")

        updatePatchStatusTheme("MaterialYou")

        addPreference(MATERIALYOU)

    }
}
