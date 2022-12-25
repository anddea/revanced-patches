package app.revanced.patches.youtube.misc.integrations.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patches.youtube.misc.integrations.fingerprints.InitFingerprint
import app.revanced.patches.youtube.misc.integrations.fingerprints.ServiceFingerprint
import app.revanced.patches.youtube.misc.integrations.fingerprints.StandalonePlayerFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.patches.integrations.AbstractIntegrationsPatch

@Name("integrations")
@YouTubeCompatibility
class IntegrationsPatch : AbstractIntegrationsPatch(
    "Lapp/revanced/integrations/utils/ReVancedUtils;",
    listOf(InitFingerprint, StandalonePlayerFingerprint, ServiceFingerprint),
)