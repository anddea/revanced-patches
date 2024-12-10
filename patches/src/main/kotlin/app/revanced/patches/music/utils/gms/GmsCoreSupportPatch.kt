package app.revanced.patches.music.utils.gms

import app.revanced.patcher.patch.Option
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.revanced.patches.music.utils.extension.sharedExtensionPatch
import app.revanced.patches.music.utils.fix.fileprovider.fileProviderPatch
import app.revanced.patches.music.utils.mainactivity.mainActivityFingerprint
import app.revanced.patches.music.utils.patch.PatchList.GMSCORE_SUPPORT
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.addGmsCorePreference
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePackageName
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.gms.gmsCoreSupportPatch
import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.revanced.util.valueOrThrow

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatch(
    fromPackageName = YOUTUBE_MUSIC_PACKAGE_NAME,
    mainActivityOnCreateFingerprint = mainActivityFingerprint.second,
    extensionPatch = sharedExtensionPatch,
    gmsCoreSupportResourcePatchFactory = ::gmsCoreSupportResourcePatch,
) {
    compatibleWith(COMPATIBLE_PACKAGE)
}

private fun gmsCoreSupportResourcePatch(
    gmsCoreVendorGroupIdOption: Option<String>,
    packageNameYouTubeOption: Option<String>,
    packageNameYouTubeMusicOption: Option<String>,
) = app.revanced.patches.shared.gms.gmsCoreSupportResourcePatch(
    fromPackageName = YOUTUBE_MUSIC_PACKAGE_NAME,
    spoofedPackageSignature = "afb0fed5eeaebdd86f56a97742f4b6b33ef59875",
    gmsCoreVendorGroupIdOption = gmsCoreVendorGroupIdOption,
    packageNameYouTubeOption = packageNameYouTubeOption,
    packageNameYouTubeMusicOption = packageNameYouTubeMusicOption,
    executeBlock = {
        updatePackageName(packageNameYouTubeMusicOption.valueOrThrow())

        addGmsCorePreference(
            CategoryType.MISC.value,
            "gms_core_settings",
            gmsCoreVendorGroupIdOption.valueOrThrow() + ".android.gms",
            "org.microg.gms.ui.SettingsActivity"
        )

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_gms_show_dialog",
            "true"
        )

        updatePatchStatus(GMSCORE_SUPPORT)

    },
) {
    dependsOn(
        baseSpoofUserAgentPatch(YOUTUBE_MUSIC_PACKAGE_NAME),
        settingsPatch,
        fileProviderPatch(
            packageNameYouTubeOption.valueOrThrow(),
            packageNameYouTubeMusicOption.valueOrThrow()
        ),
    )
}
