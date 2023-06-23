package app.revanced.patches.youtube.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerFooter
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsSubscriptionsTabletParentFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(ReelPlayerFooter) }
)