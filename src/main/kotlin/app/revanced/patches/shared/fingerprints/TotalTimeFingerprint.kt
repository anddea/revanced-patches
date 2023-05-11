package app.revanced.patches.shared.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.totalTimeId
import app.revanced.util.bytecode.isWideLiteralExists

object TotalTimeFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { it.isWideLiteralExists(totalTimeId) }
)