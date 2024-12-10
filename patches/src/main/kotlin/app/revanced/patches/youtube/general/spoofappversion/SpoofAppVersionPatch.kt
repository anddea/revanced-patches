package app.revanced.patches.youtube.general.spoofappversion

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.SPOOF_APP_VERSION
import app.revanced.patches.youtube.utils.playservice.is_18_34_or_greater
import app.revanced.patches.youtube.utils.playservice.is_18_39_or_greater
import app.revanced.patches.youtube.utils.playservice.is_18_49_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.appendAppVersion

@Suppress("unused")
val spoofAppVersionPatch = resourcePatch(
    SPOOF_APP_VERSION.title,
    SPOOF_APP_VERSION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseSpoofAppVersionPatch("$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"),
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        if (is_18_34_or_greater) {
            appendAppVersion("18.33.40")
            if (is_18_39_or_greater) {
                appendAppVersion("18.38.45")
                if (is_18_49_or_greater) {
                    appendAppVersion("18.48.39")
                }
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_APP_VERSION"
            ),
            SPOOF_APP_VERSION
        )

        // endregion

    }
}
