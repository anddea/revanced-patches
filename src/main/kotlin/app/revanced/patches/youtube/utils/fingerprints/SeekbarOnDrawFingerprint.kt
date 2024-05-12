package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object SeekbarOnDrawFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.name == "onDraw" }
)