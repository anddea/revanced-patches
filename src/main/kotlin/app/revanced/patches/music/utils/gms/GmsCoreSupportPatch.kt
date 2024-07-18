package app.revanced.patches.music.utils.gms

import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.fix.client.SpoofUserAgentPatch
import app.revanced.patches.music.utils.fix.fileprovider.FileProviderPatch
import app.revanced.patches.music.utils.integrations.IntegrationsPatch
import app.revanced.patches.music.utils.mainactivity.fingerprints.MainActivityFingerprint
import app.revanced.patches.shared.gms.BaseGmsCoreSupportPatch
import app.revanced.patches.shared.gms.BaseGmsCoreSupportResourcePatch.Companion.ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC

@Suppress("unused")
object GmsCoreSupportPatch : BaseGmsCoreSupportPatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC,
    mainActivityOnCreateFingerprint = MainActivityFingerprint,
    integrationsPatchDependency = IntegrationsPatch::class,
    dependencies = setOf(
        SpoofUserAgentPatch::class,
        FileProviderPatch::class
    ),
    gmsCoreSupportResourcePatch = GmsCoreSupportResourcePatch,
    compatiblePackages = COMPATIBLE_PACKAGE
)

