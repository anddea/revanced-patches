package app.revanced.patches.music.utils.integrations.fingerprints

import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch.IntegrationsFingerprint

object InitFingerprint : IntegrationsFingerprint(
    strings = listOf("YouTubeMusic", "activity")
)