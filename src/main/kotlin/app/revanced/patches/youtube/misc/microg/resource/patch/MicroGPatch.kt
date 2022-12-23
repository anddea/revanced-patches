package app.revanced.patches.youtube.misc.microg.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.misc.microg.bytecode.patch.MicroGBytecodePatch
import app.revanced.patches.youtube.misc.microg.shared.Constants.PACKAGE_NAME
import app.revanced.patches.youtube.misc.microg.shared.Constants.SPOOFED_PACKAGE_NAME
import app.revanced.patches.youtube.misc.microg.shared.Constants.SPOOFED_PACKAGE_SIGNATURE
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.patches.options.PatchOptions
import app.revanced.shared.util.microg.Constants.MICROG_VENDOR
import app.revanced.shared.util.microg.MicroGManifestHelper
import app.revanced.shared.util.microg.MicroGResourceHelper
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("microg-support")
@Description("Allows YouTube ReVanced to run without root and under a different package name with Vanced MicroG.")
@DependsOn(
    [
        SettingsPatch::class,
        MicroGBytecodePatch::class,
        PatchOptions::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class MicroGPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        var packageName = PatchOptions.YouTube_PackageName

        /*
         add settings
         */
        ResourceHelper.addSettings(
            context,
            "PREFERENCE_CATEGORY: MICROG_SETTINGS",
            "PREFERENCE: MICROG_SETTINGS",
            "SETTINGS: MICROG_SETTINGS"
        )

        ResourceHelper.patchSuccess(
            context,
            "microg-support"
        )

        val settingsFragment = context["res/xml/settings_fragment.xml"]
        settingsFragment.writeText(
            settingsFragment.readText().replace(
                "android:targetPackage=\"com.google.android.youtube",
                "android:targetPackage=\"$packageName"
            )
        )

        // update manifest
        MicroGResourceHelper.patchManifest(
            context,
            PACKAGE_NAME,
            "$packageName"
        )

        // add metadata to manifest
        MicroGManifestHelper.addSpoofingMetadata(
            context,
            SPOOFED_PACKAGE_NAME,
            SPOOFED_PACKAGE_SIGNATURE
        )
        return PatchResultSuccess()
    }
}