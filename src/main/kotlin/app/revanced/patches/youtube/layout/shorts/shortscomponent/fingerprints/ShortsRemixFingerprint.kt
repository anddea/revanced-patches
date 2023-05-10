package app.revanced.patches.youtube.layout.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.reelRemixId
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsRemixFingerprint : MethodFingerprint(
    customFingerprint = { it.isWideLiteralExists(reelRemixId) }
)