package app.revanced.patches.youtube.layout.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.reelPlayerFooterId
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsSubscriptionsTabletParentFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.isWideLiteralExists(reelPlayerFooterId) }
)