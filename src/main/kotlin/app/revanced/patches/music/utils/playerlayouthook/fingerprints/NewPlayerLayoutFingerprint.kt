package app.revanced.patches.music.utils.playerlayouthook.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object NewPlayerLayoutFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45399578) }
)