package app.revanced.patches.youtube.shorts.repeat.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object ReelEnumConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf(
        "REEL_LOOP_BEHAVIOR_SINGLE_PLAY",
        "REEL_LOOP_BEHAVIOR_REPEAT",
        "REEL_LOOP_BEHAVIOR_END_SCREEN"
    )
)
