package app.revanced.patches.youtube.misc.splashanimation.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object WatchWhileActivityWithInFlagsFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "onCreate"
                && methodDef.isWide32LiteralExists(45407550)
    }
)