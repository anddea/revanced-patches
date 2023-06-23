package app.revanced.patches.reddit.utils.integrations.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.fingerprints.InitFingerprint
import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch

@Name("integrations")
@RedditCompatibility
@RequiresIntegrations
class IntegrationsPatch : AbstractIntegrationsPatch(
    "Lapp/revanced/reddit/utils/ReVancedUtils;",
    listOf(InitFingerprint),
)