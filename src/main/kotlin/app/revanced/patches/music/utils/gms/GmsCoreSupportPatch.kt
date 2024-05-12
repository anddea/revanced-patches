package app.revanced.patches.music.utils.gms

import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.fix.clientspoof.ClientSpoofPatch
import app.revanced.patches.music.utils.fix.fileprovider.FileProviderPatch
import app.revanced.patches.music.utils.integrations.IntegrationsPatch
import app.revanced.patches.music.utils.mainactivity.fingerprints.MainActivityFingerprint
import app.revanced.patches.shared.gms.BaseGmsCoreSupportPatch
import app.revanced.patches.shared.packagename.PackageNamePatch
import app.revanced.patches.shared.packagename.PackageNamePatch.ORIGINAL_PACKAGE_NAME_YOUTUBE

@Suppress("unused")
object GmsCoreSupportPatch : BaseGmsCoreSupportPatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE,
    mainActivityOnCreateFingerprint = MainActivityFingerprint,
    integrationsPatchDependency = IntegrationsPatch::class,
    dependencies = setOf(ClientSpoofPatch::class, PackageNamePatch::class, FileProviderPatch::class),
    gmsCoreSupportResourcePatch = GmsCoreSupportResourcePatch,
    compatiblePackages = COMPATIBLE_PACKAGE
)

