package app.revanced.patches.youtube.utils.gms

import app.revanced.patches.shared.gms.BaseGmsCoreSupportPatch
import app.revanced.patches.shared.packagename.PackageNamePatch
import app.revanced.patches.shared.packagename.PackageNamePatch.ORIGINAL_PACKAGE_NAME_YOUTUBE
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fix.clientspoof.ClientSpoofPatch
import app.revanced.patches.youtube.utils.integrations.IntegrationsPatch
import app.revanced.patches.youtube.utils.mainactivity.fingerprints.MainActivityFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch

@Suppress("unused")
object GmsCoreSupportPatch : BaseGmsCoreSupportPatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE,
    mainActivityOnCreateFingerprint = MainActivityFingerprint,
    integrationsPatchDependency = IntegrationsPatch::class,
    dependencies = setOf(ClientSpoofPatch::class, PackageNamePatch::class, SettingsPatch::class),
    gmsCoreSupportResourcePatch = GmsCoreSupportResourcePatch,
    compatiblePackages = COMPATIBLE_PACKAGE
)
