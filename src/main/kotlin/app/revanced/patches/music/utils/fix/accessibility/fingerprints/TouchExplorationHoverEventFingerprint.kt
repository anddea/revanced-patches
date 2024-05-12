package app.revanced.patches.music.utils.fix.accessibility.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object TouchExplorationHoverEventFingerprint : MethodFingerprint(
    returnType = "Z",
    customFingerprint = { methodDef, _ -> methodDef.name == "onTouchExplorationHoverEvent" }
)