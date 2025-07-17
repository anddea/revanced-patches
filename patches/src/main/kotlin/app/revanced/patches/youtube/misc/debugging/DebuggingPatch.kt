package app.revanced.patches.youtube.misc.debugging

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.ENABLE_DEBUG_LOGGING
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val debuggingPatch = bytecodePatch(
    ENABLE_DEBUG_LOGGING.title,
    ENABLE_DEBUG_LOGGING.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: DEBUGGING"
            ),
            ENABLE_DEBUG_LOGGING
        )

        // endregion

    }
}
