package app.revanced.patches.youtube.misc.integrations.fingerprints

import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch.IntegrationsFingerprint

object InitFingerprint : IntegrationsFingerprint(
    strings = listOf("Application.onCreate")
)