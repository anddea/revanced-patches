package app.revanced.patches.youtube.utils.playercontrols.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint

internal object MotionEventFingerprint : MethodReferenceNameFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/view/MotionEvent;"),
    reference = { "setTranslationY" }
)
