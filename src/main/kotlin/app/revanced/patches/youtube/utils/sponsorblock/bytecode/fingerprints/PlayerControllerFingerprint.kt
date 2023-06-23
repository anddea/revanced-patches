package app.revanced.patches.youtube.utils.sponsorblock.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerControllerFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("SegmentPlaybackController;") && methodDef.name == "setSponsorBarRect" }
)
