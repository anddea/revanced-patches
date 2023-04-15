package app.revanced.patches.youtube.misc.integrations.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch
import app.revanced.patches.youtube.misc.integrations.fingerprints.*

@Name("integrations")
@YouTubeCompatibility
@RequiresIntegrations
class IntegrationsPatch : AbstractIntegrationsPatch(
    listOf(InitFingerprint, StandalonePlayerFingerprint, ServiceFingerprint),
)