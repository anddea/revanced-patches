package app.revanced.patches.youtube.misc.microg.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import app.revanced.patches.youtube.misc.microg.bytecode.patch.MicroGBytecodePatch
import app.revanced.patches.youtube.misc.microg.shared.Constants.PACKAGE_NAME
import app.revanced.patches.youtube.misc.microg.shared.Constants.SPOOFED_PACKAGE_NAME
import app.revanced.patches.youtube.misc.microg.shared.Constants.SPOOFED_PACKAGE_SIGNATURE
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.microg.MicroGManifestHelper.addSpoofingMetadata
import app.revanced.util.microg.MicroGResourceHelper.patchManifest
import app.revanced.util.microg.MicroGResourceHelper.patchSetting
import app.revanced.util.resources.ResourceHelper.setMicroG

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

        val packageName = PatchOptions.YouTubePackageName!!

        /*
         add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MICROG_SETTINGS",
                "PREFERENCE: MICROG_SETTINGS",
                "SETTINGS: MICROG_SETTINGS"
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

        return PatchResultSuccess()
    }
}