package app.revanced.patches.youtube.fullscreen.endscreenoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.AppRelatedEndScreenResults
import app.revanced.util.bytecode.isWideLiteralExists

object EndScreenResultsFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { it, _ -> it.isWideLiteralExists(AppRelatedEndScreenResults) }
)