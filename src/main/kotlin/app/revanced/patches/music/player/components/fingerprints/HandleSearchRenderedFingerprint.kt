package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object HandleSearchRenderedFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    customFingerprint = { methodDef, _ -> methodDef.name == "handleSearchRendered" }
)
