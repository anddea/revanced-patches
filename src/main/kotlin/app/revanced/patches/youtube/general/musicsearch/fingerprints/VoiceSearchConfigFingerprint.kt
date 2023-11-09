package app.revanced.patches.youtube.general.musicsearch.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object VoiceSearchConfigFingerprint : MethodFingerprint(
    returnType = "Z",
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45417109) }
)