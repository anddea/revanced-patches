package app.morphe.patches.reddit.layout.premiumicon

import app.morphe.util.fingerprint.legacyFingerprint

internal val premiumIconFingerprint = legacyFingerprint(
    name = "premiumIconFingerprint",
    returnType = "Z",
    customFingerprint = { method, _ ->
        method.definingClass == "Lcom/reddit/domain/model/MyAccount;" &&
                method.name == "isPremiumSubscriber"
    }
)
