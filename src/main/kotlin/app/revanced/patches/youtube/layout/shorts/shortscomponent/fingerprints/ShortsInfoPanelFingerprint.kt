package app.revanced.patches.youtube.layout.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.reelPlayerInfoPanelId
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsInfoPanelFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { it, _ -> it.isWideLiteralExists(reelPlayerInfoPanelId) }
)