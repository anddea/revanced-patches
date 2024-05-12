package app.revanced.patches.music.utils.integrations

import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.integrations.fingerprints.InitFingerprint
import app.revanced.patches.shared.integrations.BaseIntegrationsPatch

@Patch(requiresIntegrations = true)
object IntegrationsPatch : BaseIntegrationsPatch(
    setOf(InitFingerprint),
)
