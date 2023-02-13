package app.revanced.patches.music.misc.microg.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.misc.microg.shared.Constants.MUSIC_PACKAGE_NAME
import app.revanced.patches.music.misc.microg.shared.Constants.SPOOFED_PACKAGE_NAME
import app.revanced.patches.music.misc.microg.shared.Constants.SPOOFED_PACKAGE_SIGNATURE
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import app.revanced.util.microg.MicroGManifestHelper
import app.revanced.util.microg.MicroGResourceHelper

@Name("music-microg-resource-patch")
@Description("Resource patch to allow YouTube Music ReVanced to run without root and under a different package name.")
@DependsOn(
    [
        PatchOptions::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.2")
class MusicMicroGResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        val MusicPackageName = PatchOptions.Music_PackageName

        // update manifest
        MicroGResourceHelper.patchManifest(
            context,
            MUSIC_PACKAGE_NAME,
            "$MusicPackageName"
        )

        // add metadata to the manifest
        MicroGManifestHelper.addSpoofingMetadata(
            context,
            SPOOFED_PACKAGE_NAME,
            SPOOFED_PACKAGE_SIGNATURE
        )
        return PatchResultSuccess()
    }
}