package app.revanced.patches.youtube.utils.gms

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.shared.gms.BaseGmsCoreSupportResourcePatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updateGmsCorePackageName
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePackageName
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.valueOrThrow

object GmsCoreSupportResourcePatch : BaseGmsCoreSupportResourcePatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE,
    spoofedPackageSignature = "24bb24c05e47e0aefa68a58a766179d9b613a600",
    dependencies = setOf(SettingsPatch::class),
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
            PackageNameYouTube.valueOrThrow()
        )
        context.updateGmsCorePackageName(
            DEFAULT_GMS_CORE_VENDOR_GROUP_ID,
            GmsCoreVendorGroupId.valueOrThrow()
        )
    }
}
