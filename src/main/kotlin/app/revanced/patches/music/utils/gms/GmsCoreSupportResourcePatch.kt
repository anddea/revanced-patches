package app.revanced.patches.music.utils.gms

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.addMicroGPreference
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePackageName
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.gms.BaseGmsCoreSupportResourcePatch
import app.revanced.patches.shared.packagename.PackageNamePatch
import app.revanced.patches.shared.packagename.PackageNamePatch.ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC

object GmsCoreSupportResourcePatch : BaseGmsCoreSupportResourcePatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC,
    spoofedPackageSignature = "afb0fed5eeaebdd86f56a97742f4b6b33ef59875",
    dependencies = setOf(PackageNamePatch::class, SettingsPatch::class),
) {
    private const val GMS_CORE_PACKAGE_NAME = "app.revanced.android.gms"
    private const val GMS_CORE_SETTINGS_ACTIVITY = "org.microg.gms.ui.SettingsActivity"

    override fun execute(context: ResourceContext) {
        super.execute(context)

        context.updatePackageName(PackageNamePatch.packageNameYouTubeMusic)

        context.addMicroGPreference(
            CategoryType.MISC.value,
            "gms_core_settings",
            GMS_CORE_PACKAGE_NAME,
            GMS_CORE_SETTINGS_ACTIVITY
        )
    }
}
