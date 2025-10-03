import app.revanced.util.fingerprint.legacyFingerprint

internal val premiumCheckFingerprint = legacyFingerprint(
    name = "premiumCheckFingerprint",
    returnType = "V",
    parameters = listOf("I"),
    strings = listOf("ODAz")
)

internal val integrityCheckFingerprint = legacyFingerprint(
    name = "integrityCheckFingerprint",
    returnType = "Ljava/lang/Object;",
    strings = listOf(
        "System.exit returned normally, while it was supposed to halt JVM."
    )
)
