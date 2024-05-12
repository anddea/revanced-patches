package app.revanced.patches.music.utils.fix.fileprovider.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object FileProviderResolverFingerprint : MethodFingerprint(
    returnType = "L",
    strings = listOf(
        "android.support.FILE_PROVIDER_PATHS",
        "Name must not be empty"
    )
)