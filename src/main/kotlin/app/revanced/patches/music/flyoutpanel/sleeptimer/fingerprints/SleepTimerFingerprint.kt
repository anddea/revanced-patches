package app.revanced.patches.music.flyoutpanel.sleeptimer.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object SleepTimerFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45372767) }
)