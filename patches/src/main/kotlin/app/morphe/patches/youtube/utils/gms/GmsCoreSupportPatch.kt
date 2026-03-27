package app.morphe.patches.youtube.utils.gms

import app.morphe.patcher.patch.Option
import app.morphe.patches.shared.gms.gmsCoreSupportPatch
import app.morphe.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.fix.streamingdata.spoofStreamingDataPatch
import app.morphe.patches.youtube.utils.mainactivity.mainActivityFingerprint
import app.morphe.patches.youtube.utils.patch.PatchList.GMSCORE_SUPPORT
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.ResourceUtils.updateGmsCorePackageName
import app.morphe.patches.youtube.utils.settings.ResourceUtils.updatePackageName
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.valueOrThrow

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
) = app.morphe.patches.shared.gms.gmsCoreSupportResourcePatch(
    fromPackageName = YOUTUBE_PACKAGE_NAME,
    spoofedPackageSignature = "24bb24c05e47e0aefa68a58a766179d9b613a600",
    gmsCoreVendorGroupIdOption = gmsCoreVendorGroupIdOption,
    packageNameYouTubeOption = packageNameYouTubeOption,
    packageNameYouTubeMusicOption = packageNameYouTubeMusicOption,
    executeBlock = {
        updatePackageName(
            packageNameYouTubeOption.valueOrThrow()
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
