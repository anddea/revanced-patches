package app.revanced.patches.youtube.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.RightComment
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsCommentFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.isWideLiteralExists(RightComment) }
)