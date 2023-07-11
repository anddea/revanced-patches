package app.revanced.patches.reddit.utils.integrations.patch

import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.reddit.utils.integrations.fingerprints.InitFingerprint
import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch

@RequiresIntegrations
class IntegrationsPatch : AbstractIntegrationsPatch(
    "Lapp/revanced/reddit/utils/ReVancedUtils;",
    listOf(InitFingerprint),
)