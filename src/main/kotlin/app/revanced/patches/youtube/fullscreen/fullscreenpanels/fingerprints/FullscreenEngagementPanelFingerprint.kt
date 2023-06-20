package app.revanced.patches.youtube.fullscreen.fullscreenpanels.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.FullScreenEngagementPanel
import app.revanced.util.bytecode.isWideLiteralExists

object FullscreenEngagementPanelFingerprint : MethodFingerprint(
    returnType = "L",
    customFingerprint = { it, _ ->
        it.definingClass.endsWith("FullscreenEngagementPanelOverlay;") && it.isWideLiteralExists(
            FullScreenEngagementPanel
        )
    }
)