package app.revanced.patches.youtube.general.downloads.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object OfflinePlaylistEndpointFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Object is not an offlineable playlist: ")
)
