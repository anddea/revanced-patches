package app.revanced.patches.youtube.utils.gms

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.shared.gms.BaseGmsCoreSupportResourcePatch
import app.revanced.patches.shared.packagename.PackageNamePatch
import app.revanced.patches.shared.packagename.PackageNamePatch.ORIGINAL_PACKAGE_NAME_YOUTUBE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePackageName
import app.revanced.patches.youtube.utils.settings.SettingsPatch

object GmsCoreSupportResourcePatch : BaseGmsCoreSupportResourcePatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE,
    spoofedPackageSignature = "24bb24c05e47e0aefa68a58a766179d9b613a600",
    dependencies = setOf(PackageNamePatch::class, SettingsPatch::class),
) {
    override fun execute(context: ResourceContext) {
        super.execute(context)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GMS_CORE_SETTINGS"
            )
        )
        SettingsPatch.updatePatchStatus("GmsCore support")

        context.updatePackageName(
            ORIGINAL_PACKAGE_NAME_YOUTUBE,
            PackageNamePatch.packageNameYouTube
        )
    }
}
