package app.revanced.patches.youtube.layout.fullscreen.endscreenoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.appRelatedEndScreenResultsId
import app.revanced.util.bytecode.isWideLiteralExists

object EndScreenResultsFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { it, _ -> it.isWideLiteralExists(appRelatedEndScreenResultsId) }
)