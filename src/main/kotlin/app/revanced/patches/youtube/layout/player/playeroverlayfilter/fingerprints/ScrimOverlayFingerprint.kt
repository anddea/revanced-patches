package app.revanced.patches.youtube.layout.player.playeroverlayfilter.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.scrimOverlayId
import app.revanced.util.bytecode.isWideLiteralExists

object ScrimOverlayFingerprint : MethodFingerprint(
    customFingerprint = { it.definingClass.endsWith("YouTubeControlsOverlay;") && it.isWideLiteralExists(scrimOverlayId) }
)