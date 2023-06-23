package app.revanced.patches.youtube.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelDynRemix
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsRemixFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(ReelDynRemix) }
)