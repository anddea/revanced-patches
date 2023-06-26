package app.revanced.patches.youtube.shorts.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelRightLikeIcon
import app.revanced.util.bytecode.isWideLiteralExists

object ShortsLikeFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Z", "Z", "L"),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(ReelRightLikeIcon) }
)