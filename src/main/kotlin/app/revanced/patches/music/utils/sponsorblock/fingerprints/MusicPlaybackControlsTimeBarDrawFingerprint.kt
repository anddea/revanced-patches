package app.revanced.patches.music.utils.sponsorblock.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object MusicPlaybackControlsTimeBarDrawFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/MusicPlaybackControlsTimeBar;")
                && methodDef.name == "draw"
    }
)