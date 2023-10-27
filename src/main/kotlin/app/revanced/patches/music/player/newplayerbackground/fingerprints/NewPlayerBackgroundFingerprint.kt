package app.revanced.patches.music.player.newplayerbackground.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object NewPlayerBackgroundFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45415319) }
)