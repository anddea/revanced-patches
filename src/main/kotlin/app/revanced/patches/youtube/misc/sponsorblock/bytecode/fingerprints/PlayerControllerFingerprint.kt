package app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerControllerFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.definingClass.endsWith("SegmentPlaybackController;") && it.name == "setSponsorBarRect" }
)
