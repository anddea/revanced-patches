package app.revanced.patches.reddit.layout.premiumicon

import app.revanced.util.fingerprint.legacyFingerprint

internal val premiumIconFingerprint = legacyFingerprint(
    name = "premiumIconFingerprint",
    returnType = "Z",
    customFingerprint = { method, classDef ->
        method.definingClass.endsWith("/MyAccount;") &&
                method.name == "isPremiumSubscriber" &&
                classDef.sourceFile == "MyAccount.kt"
    }
)
