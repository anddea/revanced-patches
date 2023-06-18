package app.revanced.patches.youtube.misc.integrations.fingerprints

import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch.IntegrationsFingerprint

object ServiceFingerprint : IntegrationsFingerprint(
    customFingerprint = { it, _ -> it.definingClass.endsWith("ApiPlayerService;") && it.name == "<init>" },
    contextRegisterResolver = { it.implementation!!.registerCount - it.parameters.size }
)