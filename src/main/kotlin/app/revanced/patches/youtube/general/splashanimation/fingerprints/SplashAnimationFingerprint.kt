package app.revanced.patches.youtube.general.splashanimation.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.DarkSplashAnimation
import app.revanced.util.containsWideLiteralInstructionIndex

internal object SplashAnimationFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "onCreate"
                && methodDef.containsWideLiteralInstructionIndex(DarkSplashAnimation)
    }
)