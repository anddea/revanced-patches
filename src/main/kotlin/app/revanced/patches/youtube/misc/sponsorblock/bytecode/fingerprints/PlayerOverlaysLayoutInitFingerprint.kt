package app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerOverlaysLayoutInitFingerprint : MethodFingerprint(

    customFingerprint = { it.returnType.endsWith("YouTubePlayerOverlaysLayout;") }
)