package app.revanced.patches.youtube.utils.microg.bytecode.fingerprints


import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object CastContextFetchFingerprint : MethodFingerprint(
    strings = listOf("Error fetching CastContext.")
)