package app.revanced.patches.music.utils.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object PendingIntentReceiverFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("YTM Dislike", "YTM Next", "YTM Previous"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/PendingIntentReceiver;")
    }
)