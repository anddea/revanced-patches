package app.revanced.patches.music.misc.quality.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch.Companion.qualityAutoId
import app.revanced.util.bytecode.isWideLiteralExists

object MusicVideoQualitySetterParentFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    customFingerprint = { it, _ -> it.isWideLiteralExists(qualityAutoId)}
)