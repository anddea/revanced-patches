package app.revanced.patches.music.utils.microg.resource.patch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.utils.microg.shared.Constants.MUSIC_PACKAGE_NAME
import app.revanced.patches.music.utils.microg.shared.Constants.SPOOFED_PACKAGE_NAME
import app.revanced.patches.music.utils.microg.shared.Constants.SPOOFED_PACKAGE_SIGNATURE
import app.revanced.patches.shared.patch.packagename.PackageNamePatch
import app.revanced.util.microg.MicroGManifestHelper.addSpoofingMetadata
import app.revanced.util.microg.MicroGResourceHelper.patchManifest
import app.revanced.util.resources.MusicResourceHelper.setMicroG

@DependsOn([PackageNamePatch::class])
class MicroGResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {
        val packageName = PackageNamePatch.MusicPackageName
            ?: throw PatchException("Invalid package name.")

        if (packageName == MUSIC_PACKAGE_NAME)
            throw PatchException("Original package name is not available as package name for MicroG build.")

        // update manifest
        context.patchManifest(
            MUSIC_PACKAGE_NAME,
            packageName
        )

        // add metadata to the manifest
        context.addSpoofingMetadata(
            SPOOFED_PACKAGE_NAME,
            SPOOFED_PACKAGE_SIGNATURE
        )

        context.setMicroG(packageName)

    }
}