package app.revanced.patches.youtube.misc.ambientmode.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object AmbientModeInFullscreenFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.isWide32LiteralExists(45389368)
    }
)