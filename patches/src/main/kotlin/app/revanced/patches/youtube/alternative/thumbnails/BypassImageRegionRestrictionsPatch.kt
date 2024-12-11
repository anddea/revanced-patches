package app.revanced.patches.youtube.alternative.thumbnails

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.imageurl.addImageUrlHook
import app.revanced.patches.shared.imageurl.cronetImageUrlHookPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.BYPASS_IMAGE_REGION_RESTRICTIONS
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val bypassImageRegionRestrictionsPatch = bytecodePatch(
    BYPASS_IMAGE_REGION_RESTRICTIONS.title,
    BYPASS_IMAGE_REGION_RESTRICTIONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        cronetImageUrlHookPatch(true),
        settingsPatch,
    )
    execute {

        addImageUrlHook()

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ALTERNATIVE_THUMBNAILS",
                "SETTINGS: BYPASS_IMAGE_REGION_RESTRICTIONS"
            ),
            BYPASS_IMAGE_REGION_RESTRICTIONS
        )

        // endregion

    }
}
