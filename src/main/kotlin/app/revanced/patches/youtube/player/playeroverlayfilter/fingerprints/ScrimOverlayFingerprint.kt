package app.revanced.patches.youtube.player.playeroverlayfilter.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ScrimOverlay
import app.revanced.util.bytecode.isWideLiteralExists

object ScrimOverlayFingerprint : MethodFingerprint(
    customFingerprint = { it, _ ->
        it.definingClass.endsWith("YouTubeControlsOverlay;") && it.isWideLiteralExists(
            ScrimOverlay
        )
    }
)