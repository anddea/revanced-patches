package app.revanced.patches.youtube.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerInfoPanel
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsInfoPanelFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { it, _ -> it.isWideLiteralExists(ReelPlayerInfoPanel) }
)