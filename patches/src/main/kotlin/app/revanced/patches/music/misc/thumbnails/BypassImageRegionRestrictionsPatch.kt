package app.revanced.patches.music.misc.thumbnails

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.BYPASS_IMAGE_REGION_RESTRICTIONS
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.imageurl.addImageUrlHook
import app.revanced.patches.shared.imageurl.cronetImageUrlHookPatch

@Suppress("unused")
val bypassImageRegionRestrictionsPatch = bytecodePatch(
    BYPASS_IMAGE_REGION_RESTRICTIONS.title,
    BYPASS_IMAGE_REGION_RESTRICTIONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        cronetImageUrlHookPatch(false)
    )

    execute {
        addImageUrlHook()

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_bypass_image_region_restrictions",
            "false"
        )

        updatePatchStatus(BYPASS_IMAGE_REGION_RESTRICTIONS)

    }
}