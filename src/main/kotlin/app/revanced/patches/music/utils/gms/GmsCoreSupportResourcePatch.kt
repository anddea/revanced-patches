package app.revanced.patches.music.utils.gms

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.addMicroGPreference
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePackageName
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.gms.BaseGmsCoreSupportResourcePatch
import app.revanced.util.valueOrThrow

object GmsCoreSupportResourcePatch : BaseGmsCoreSupportResourcePatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC,
    spoofedPackageSignature = "afb0fed5eeaebdd86f56a97742f4b6b33ef59875",
    dependencies = setOf(SettingsPatch::class),
) {
    private const val GMS_CORE_SETTINGS_ACTIVITY = "org.microg.gms.ui.SettingsActivity"

    override fun execute(context: ResourceContext) {
        super.execute(context)

        context.updatePackageName(PackageNameYouTubeMusic.valueOrThrow())

        context.addMicroGPreference(
            CategoryType.MISC.value,
            "gms_core_settings",
            GmsCoreVendorGroupId.valueOrThrow(),
            GMS_CORE_SETTINGS_ACTIVITY
        )

        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_gms_show_dialog",
            "true"
        )
    }
}
