package app.morphe.patches.youtube.general.spoofappversion

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.SPOOF_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.SPOOF_APP_VERSION
import app.morphe.patches.youtube.utils.playservice.is_19_26_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_00_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_31_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_05_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.Utils.printWarn
import app.morphe.util.findMethodOrThrow
import app.morphe.util.insertLiteralOverride
import app.morphe.util.returnEarly

private const val SPOOF_EXTENSION_CLASS_DESCRIPTOR = "$SPOOF_PATH/SpoofAppVersionPatch;"

private val spoofAppVersionBytecodePatch = bytecodePatch(
    description = "spoofAppVersionBytecodePatch"
) {

    dependsOn(
        settingsPatch,
        versionCheckPatch,
        clientContextHookPatch,
    )

    execute {
        if (!is_19_26_or_greater) {
            return@execute
        }

        findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
            name == "SpoofAppVersion"
        }.returnEarly(true)

        if (!is_20_00_or_greater) {
            findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
                name == "SpoofAppVersionDefaultBoolean"
            }.returnEarly(true)
        }

        /**
         * Fix Shorts no overlay when a modern YouTube client is spoofed to an older version.
         * The legacy Reel response is not understood by YouTube 20.05 and newer, so only Reel
         * requests use the oldest client version that still returns the modern overlay response.
         */
        if (is_21_05_or_greater) {
            addClientVersionHook(
                Endpoint.REEL,
                "$SPOOF_EXTENSION_CLASS_DESCRIPTOR->getShortsAppVersionOverride(Ljava/lang/String;)Ljava/lang/String;",
            )
        } else if (is_20_31_or_greater) {
            listOf(
                ShortsBoldIconsPrimaryFeatureFlagFingerprint,
                ShortsBoldIconsSecondaryFeatureFlagFingerprint,
            ).forEach { fingerprint ->
                fingerprint.method.insertLiteralOverride(
                    fingerprint.instructionMatches.first().index,
                    "$SPOOF_EXTENSION_CLASS_DESCRIPTOR->disableShortsBoldIcons(Z)Z",
                )
            }
        }
    }

}

@Suppress("unused")
val spoofAppVersionPatch = resourcePatch(
    SPOOF_APP_VERSION.title,
    SPOOF_APP_VERSION.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        baseSpoofAppVersionPatch("$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"),
        spoofAppVersionBytecodePatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        if (!is_19_26_or_greater) {
            printWarn("\"${SPOOF_APP_VERSION.title}\" is not supported in this version. Use YouTube 19.43.41 or later.")
            return@execute
        }

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_APP_VERSION"
            ),
            SPOOF_APP_VERSION
        )
    }
}
