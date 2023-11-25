package app.revanced.patches.music.utils.microg

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.microg.Constants.MUSIC_PACKAGE_NAME
import app.revanced.patches.music.utils.microg.Constants.SPOOFED_PACKAGE_NAME
import app.revanced.patches.music.utils.microg.Constants.SPOOFED_PACKAGE_SIGNATURE
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.patch.packagename.PackageNamePatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.microg.Constants.MICROG_PACKAGE_NAME
import app.revanced.util.microg.MicroGManifestHelper.addSpoofingMetadata
import app.revanced.util.microg.MicroGResourceHelper.patchManifest
import app.revanced.util.resources.MusicResourceHelper.addMicroGPreference
import app.revanced.util.resources.MusicResourceHelper.setMicroG

@Patch(
    dependencies = [
        PackageNamePatch::class,
        SettingsPatch::class
    ]
)
object MicroGResourcePatch : ResourcePatch() {
    private const val MICROG_TARGET_CLASS = "org.microg.gms.ui.SettingsActivity"
    override fun execute(context: ResourceContext) {
        val packageName = PackageNamePatch.PackageNameYouTubeMusic
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

        context.addMicroGPreference(
            CategoryType.MISC.value,
            "microg_settings",
            MICROG_PACKAGE_NAME,
            MICROG_TARGET_CLASS
        )

    }
}