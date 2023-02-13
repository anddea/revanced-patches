package app.revanced.patches.music.misc.integrations.fingerprints

import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch.IntegrationsFingerprint

object InitFingerprint : IntegrationsFingerprint(
    strings = listOf("YouTubeMusic", "activity")
)