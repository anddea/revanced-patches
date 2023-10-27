package app.revanced.patches.youtube.general.loadingscreen.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object GradientLoadingScreenPrimaryFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45412406) }
)