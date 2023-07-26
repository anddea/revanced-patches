package app.revanced.patches.youtube.misc.splashanimation.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object SplashAnimationBuilderFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/WatchWhileActivity;")
                && methodDef.name == "onCreate"
                && methodDef.isWide32LiteralExists(45407550)
    }
)