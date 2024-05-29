package app.revanced.patches.shared.litho.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

/**
 * Since YouTube v19.18.41 and YT Music 7.01.53, pathBuilder is being handled by a different Method.
 */
internal object PathBuilderFingerprint : MethodFingerprint(
    returnType = "L",
    strings = listOf("Number of bits must be positive")
)