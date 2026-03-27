package app.morphe.patches.music.misc.thumbnails

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.patch.PatchList.BYPASS_IMAGE_REGION_RESTRICTIONS
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.imageurl.addImageUrlHook
import app.morphe.patches.shared.imageurl.cronetImageUrlHookPatch

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