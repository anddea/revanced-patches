package app.morphe.patches.youtube.layout.theme

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.materialyou.baseMaterialYou
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.MATERIALYOU
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusTheme
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.copyXmlNode

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
        baseMaterialYou()

        copyXmlNode("youtube/materialyou/host", "values-v31/colors.xml", "resources")

        updatePatchStatusTheme("MaterialYou")

        addPreference(MATERIALYOU)

    }
}
