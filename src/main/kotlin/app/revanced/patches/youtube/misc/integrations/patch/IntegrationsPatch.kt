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
    "Lapp/revanced/integrations/utils/ReVancedUtils;",
    listOf(InitFingerprint, StandalonePlayerFingerprint, ServiceFingerprint),
)