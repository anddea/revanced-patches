package app.revanced.patches.youtube.fullscreen.endscreenoverlay.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AppRelatedEndScreenResults
import app.revanced.util.bytecode.isWideLiteralExists

object EndScreenResultsParentFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(AppRelatedEndScreenResults) }
)