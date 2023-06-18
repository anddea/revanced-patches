package app.revanced.patches.music.misc.quality.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch.Companion.qualityTitleId
import app.revanced.util.bytecode.isWideLiteralExists

object MusicVideoQualitySettingsParentFingerprint : MethodFingerprint(
    returnType = "L",
    parameters = listOf(),
    customFingerprint = { it, _ -> it.isWideLiteralExists(qualityTitleId)}
)