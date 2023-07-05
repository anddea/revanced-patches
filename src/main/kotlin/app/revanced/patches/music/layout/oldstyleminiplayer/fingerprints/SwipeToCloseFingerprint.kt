package app.revanced.patches.music.layout.oldstyleminiplayer.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object SwipeToCloseFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45398432) }
)