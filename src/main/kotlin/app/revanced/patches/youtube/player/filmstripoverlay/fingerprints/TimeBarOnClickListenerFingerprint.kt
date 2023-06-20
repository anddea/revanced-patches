package app.revanced.patches.youtube.player.filmstripoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.WatchWhileTimeBarOverlayStub
import app.revanced.util.bytecode.isWideLiteralExists

object TimeBarOnClickListenerFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { it, _ ->
        it.definingClass.endsWith("YouTubeControlsOverlay;") && it.isWideLiteralExists(
            WatchWhileTimeBarOverlayStub
        )
    }
)