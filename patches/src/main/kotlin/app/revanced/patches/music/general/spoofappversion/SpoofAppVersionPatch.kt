package app.revanced.patches.music.general.spoofappversion

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.music.general.oldstylelibraryshelf.oldStyleLibraryShelfPatch
import app.revanced.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.revanced.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.patch.PatchList.SPOOF_APP_VERSION
import app.revanced.patches.music.utils.playservice.is_7_17_or_greater
import app.revanced.patches.music.utils.playservice.is_7_25_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.revanced.util.Utils.printWarn
import app.revanced.util.appendAppVersion
import app.revanced.util.findMethodOrThrow

private var defaultValue = "false"

private val spoofAppVersionBytecodePatch = bytecodePatch(
    description = "spoofAppVersionBytecodePatch"
) {
    dependsOn(
        baseSpoofAppVersionPatch("$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"),
        versionCheckPatch,
    )

    execute {
        if (is_7_25_or_greater) {
            return@execute
        }
        if (is_7_17_or_greater) {
            findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
                name == "SpoofAppVersionDefaultString"
            }.replaceInstruction(
                0,
                "const-string v0, \"7.16.53\""
            )
            findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
                name == "SpoofAppVersionDefaultBoolean"
            }.replaceInstruction(
                0,
                "const/4 v0, 0x1"
            )

            defaultValue = "true"
        }
    }
}

@Suppress("unused")
val spoofAppVersionPatch = resourcePatch(
    SPOOF_APP_VERSION.title,
    SPOOF_APP_VERSION.summary,
) {
    compatibleWith(
        YOUTUBE_MUSIC_PACKAGE_NAME(
            "6.20.51",
            "6.29.59",
            "6.42.55",
            "6.51.53",
            "7.16.53",
        ),
    )

    dependsOn(
        spoofAppVersionBytecodePatch,
        oldStyleLibraryShelfPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        if (is_7_25_or_greater) {
            printWarn("\"${SPOOF_APP_VERSION.title}\" is not supported in this version. Use YouTube Music 7.24.51 or earlier.")
            return@execute
        }
        if (is_7_17_or_greater) {
            appendAppVersion("7.16.53")
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_spoof_app_version",
            defaultValue
        )
        addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_spoof_app_version_target",
            "revanced_spoof_app_version"
        )

        updatePatchStatus(SPOOF_APP_VERSION)

    }
}
