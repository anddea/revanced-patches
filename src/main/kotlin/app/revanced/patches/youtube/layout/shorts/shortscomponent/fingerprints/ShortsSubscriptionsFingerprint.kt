package app.revanced.patches.youtube.layout.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.reelPlayerPausedId
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsSubscriptionsFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.isWideLiteralExists(reelPlayerPausedId) }
)