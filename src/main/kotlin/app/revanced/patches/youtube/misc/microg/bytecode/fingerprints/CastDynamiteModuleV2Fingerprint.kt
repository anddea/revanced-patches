package app.revanced.patches.youtube.misc.microg.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object CastDynamiteModuleV2Fingerprint : MethodFingerprint(
    strings = listOf("Failed to load module via V2: ")
)