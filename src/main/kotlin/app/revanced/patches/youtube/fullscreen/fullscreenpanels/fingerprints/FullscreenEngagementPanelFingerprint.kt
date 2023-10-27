package app.revanced.patches.youtube.fullscreen.fullscreenpanels.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FullScreenEngagementPanel
import app.revanced.util.bytecode.isWideLiteralExists

object FullscreenEngagementPanelFingerprint : MethodFingerprint(
    returnType = "L",
    parameters = listOf("L"),
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(
            FullScreenEngagementPanel
        )
    }
)