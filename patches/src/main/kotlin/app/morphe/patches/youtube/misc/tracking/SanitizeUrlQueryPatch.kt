package app.morphe.patches.youtube.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.tracking.baseSanitizeUrlQueryPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.SANITIZE_SHARING_LINKS
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val sanitizeUrlQueryPatch = bytecodePatch(
    SANITIZE_SHARING_LINKS.title,
    SANITIZE_SHARING_LINKS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseSanitizeUrlQueryPatch,
        settingsPatch,
    )

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: SANITIZE_SHARING_LINKS"
            ),
            SANITIZE_SHARING_LINKS
        )

        // endregion

    }
}
