package app.revanced.patches.youtube.general.loadingscreen.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object GradientLoadingScreenSecondaryFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45418917) }
)