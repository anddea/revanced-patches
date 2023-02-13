package app.revanced.patches.youtube.misc.integrations.fingerprints

import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch.IntegrationsFingerprint

object StandalonePlayerFingerprint : IntegrationsFingerprint(
    strings = listOf(
        "Invalid PlaybackStartDescriptor. Returning the instance itself.",
        "com.google.android.music",
    ),
)