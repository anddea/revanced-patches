package app.revanced.patches.music.utils.integrations

import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.integrations.fingerprints.InitFingerprint
import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch
import app.revanced.util.integrations.Constants.MUSIC_INTEGRATIONS_PATH

@Patch(requiresIntegrations = true)
object IntegrationsPatch : AbstractIntegrationsPatch(
    "$MUSIC_INTEGRATIONS_PATH/utils/ReVancedUtils;",
    setOf(InitFingerprint),
)
