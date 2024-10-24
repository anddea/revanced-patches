package app.revanced.patches.shared.gms.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object GmsServiceBrokerFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("mServiceBroker is null, client disconnected")
)