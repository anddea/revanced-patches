package app.morphe.patches.music.utils.gms

import app.morphe.patcher.patch.Option
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.fix.fileprovider.fileProviderPatch
import app.morphe.patches.music.utils.fix.streamingdata.spoofStreamingDataPatch
import app.morphe.patches.music.utils.mainactivity.mainActivityFingerprint
import app.morphe.patches.music.utils.patch.PatchList.GMSCORE_SUPPORT
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePackageName
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.gms.gmsCoreSupportPatch
import app.morphe.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.morphe.util.valueOrThrow

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
) = app.morphe.patches.shared.gms.gmsCoreSupportResourcePatch(
    fromPackageName = YOUTUBE_MUSIC_PACKAGE_NAME,
    spoofedPackageSignature = "afb0fed5eeaebdd86f56a97742f4b6b33ef59875",
    gmsCoreVendorGroupIdOption = gmsCoreVendorGroupIdOption,
    packageNameYouTubeOption = packageNameYouTubeOption,
    packageNameYouTubeMusicOption = packageNameYouTubeMusicOption,
    executeBlock = {
        updatePackageName(
            gmsCoreVendorGroupIdOption.valueOrThrow() + ".android.gms",
            packageNameYouTubeMusicOption.valueOrThrow()
        )

        updatePatchStatus(GMSCORE_SUPPORT)

    },
) {
    dependsOn(
        baseSpoofUserAgentPatch(YOUTUBE_MUSIC_PACKAGE_NAME),
        spoofStreamingDataPatch,
        settingsPatch,
        fileProviderPatch(
            packageNameYouTubeOption.valueOrThrow(),
            packageNameYouTubeMusicOption.valueOrThrow()
        ),
    )
}
