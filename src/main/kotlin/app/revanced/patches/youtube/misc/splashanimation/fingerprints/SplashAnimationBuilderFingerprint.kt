package app.revanced.patches.youtube.misc.splashanimation.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object SplashAnimationBuilderFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { it, _ -> it.isWide32LiteralExists(45407550) }
)