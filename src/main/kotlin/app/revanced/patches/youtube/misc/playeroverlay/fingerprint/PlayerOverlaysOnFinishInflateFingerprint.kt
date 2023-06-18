package app.revanced.patches.youtube.misc.playeroverlay.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerOverlaysOnFinishInflateFingerprint : MethodFingerprint(
    customFingerprint = { it, _ ->
        it.definingClass.endsWith("YouTubePlayerOverlaysLayout;") && it.name == "onFinishInflate"
    }
)
