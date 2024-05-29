package app.revanced.patches.music.utils.sponsorblock.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object SeekbarOnDrawFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.name == "onDraw" }
)