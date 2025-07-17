package app.revanced.patches.youtube.utils.gms

import app.revanced.patcher.patch.Option
import app.revanced.patches.shared.gms.gmsCoreSupportPatch
import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.fix.streamingdata.spoofStreamingDataPatch
import app.revanced.patches.youtube.utils.mainactivity.mainActivityFingerprint
import app.revanced.patches.youtube.utils.patch.PatchList.GMSCORE_SUPPORT
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updateGmsCorePackageName
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePackageName
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.valueOrThrow

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatch(
    fromPackageName = YOUTUBE_PACKAGE_NAME,
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
    fromPackageName = YOUTUBE_PACKAGE_NAME,
    spoofedPackageSignature = "24bb24c05e47e0aefa68a58a766179d9b613a600",
    gmsCoreVendorGroupIdOption = gmsCoreVendorGroupIdOption,
    packageNameYouTubeOption = packageNameYouTubeOption,
    packageNameYouTubeMusicOption = packageNameYouTubeMusicOption,
    executeBlock = {
        updatePackageName(
            YOUTUBE_PACKAGE_NAME,
            packageNameYouTubeOption.valueOrThrow(),
            packageNameYouTubeMusicOption.valueOrThrow()
        )
        updateGmsCorePackageName(
            "app.revanced",
            gmsCoreVendorGroupIdOption.valueOrThrow()
        )
        addPreference(
            arrayOf(
                "PREFERENCE: GMS_CORE_SETTINGS"
            ),
            GMSCORE_SUPPORT
        )
    },
) {
    dependsOn(
        baseSpoofUserAgentPatch(YOUTUBE_PACKAGE_NAME),
        spoofStreamingDataPatch,
        settingsPatch,
        accountCredentialsInvalidTextPatch,
    )
}
