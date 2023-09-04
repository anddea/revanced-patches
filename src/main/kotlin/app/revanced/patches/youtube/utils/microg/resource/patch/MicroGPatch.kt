package app.revanced.patches.youtube.utils.microg.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.patch.packagename.PackageNamePatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.microg.bytecode.patch.MicroGBytecodePatch
import app.revanced.patches.youtube.utils.microg.shared.Constants.PACKAGE_NAME
import app.revanced.patches.youtube.utils.microg.shared.Constants.SPOOFED_PACKAGE_NAME
import app.revanced.patches.youtube.utils.microg.shared.Constants.SPOOFED_PACKAGE_SIGNATURE
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.microg.MicroGManifestHelper.addSpoofingMetadata
import app.revanced.util.microg.MicroGResourceHelper.patchManifest
import app.revanced.util.microg.MicroGResourceHelper.patchSetting
import app.revanced.util.resources.ResourceHelper.setMicroG

@Patch
@Name("MicroG support")
@Description("Allows ReVanced to run without root and under a different package name with MicroG.")
@DependsOn(
    [
        PackageNamePatch::class,
        SettingsPatch::class,
        MicroGBytecodePatch::class,
    ]
)
@YouTubeCompatibility
class MicroGPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        val packageName = PackageNamePatch.YouTubePackageName
            ?: throw PatchException("Invalid package name.")

        if (packageName == PACKAGE_NAME)
            throw PatchException("Original package name is not available as package name for MicroG build.")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: MICROG_SETTINGS"
            )
        )
        SettingsPatch.updatePatchStatus("microg-support")

        // update settings fragment
        context.patchSetting(
            PACKAGE_NAME,
            packageName
        )

        // update manifest
        context.patchManifest(
            PACKAGE_NAME,
            packageName
        )

        // add metadata to manifest
        context.addSpoofingMetadata(
            SPOOFED_PACKAGE_NAME,
            SPOOFED_PACKAGE_SIGNATURE
        )

        setMicroG(packageName)

    }
}