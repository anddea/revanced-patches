package app.revanced.patches.youtube.utils.gms

import app.revanced.patches.shared.gms.BaseGmsCoreSupportPatch
import app.revanced.patches.shared.gms.BaseGmsCoreSupportResourcePatch.Companion.ORIGINAL_PACKAGE_NAME_YOUTUBE
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fix.client.SpoofClientPatch
import app.revanced.patches.youtube.utils.fix.client.SpoofUserAgentPatch
import app.revanced.patches.youtube.utils.integrations.IntegrationsPatch
import app.revanced.patches.youtube.utils.mainactivity.fingerprints.MainActivityFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch

@Suppress("unused")
object GmsCoreSupportPatch : BaseGmsCoreSupportPatch(
    fromPackageName = ORIGINAL_PACKAGE_NAME_YOUTUBE,
    mainActivityOnCreateFingerprint = MainActivityFingerprint,
    integrationsPatchDependency = IntegrationsPatch::class,
    dependencies = setOf(
        SpoofClientPatch::class,
        SpoofUserAgentPatch::class,
        SettingsPatch::class
    ),
    gmsCoreSupportResourcePatch = GmsCoreSupportResourcePatch,
    compatiblePackages = COMPATIBLE_PACKAGE
)
