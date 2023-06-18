package app.revanced.patches.youtube.layout.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.reelPlayerBadgeId
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsPaidContentFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.isWideLiteralExists(reelPlayerBadgeId) }
)