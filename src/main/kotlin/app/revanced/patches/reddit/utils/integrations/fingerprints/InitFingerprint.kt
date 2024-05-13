package app.revanced.patches.reddit.utils.integrations.fingerprints

import app.revanced.patches.shared.integrations.BaseIntegrationsPatch.IntegrationsFingerprint

internal object InitFingerprint : IntegrationsFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/FrontpageApplication;") &&
                methodDef.name == "onCreate"
    }
)