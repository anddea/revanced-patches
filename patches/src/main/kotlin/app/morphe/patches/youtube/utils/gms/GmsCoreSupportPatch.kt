/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.Option
import app.morphe.patches.shared.gms.gmsCoreSupportPatch
import app.morphe.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
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
    executeBlock = {
        // GmsCore allows the capabilities it can not look up, which makes every account a supervised account.
        AccountHasCapabilitiesFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static/range { p2 .. p2 }, Lapp/morphe/extension/shared/patches/GmsCoreSupport;->denySupervisedAccountCapability([Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :ask_gms_core
                # Same result as Google Play services for accounts without the capability.
                const/4 v0, 0x2
                invoke-static { v0 }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object v0
                return-object v0
                :ask_gms_core
                nop
            """
        )
    },
) {
    dependsOn(deviceComplianceCheckPatch)

    compatibleWith(COMPATIBILITY_YOUTUBE)
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
