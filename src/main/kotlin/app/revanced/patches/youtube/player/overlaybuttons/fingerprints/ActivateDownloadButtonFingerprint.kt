package app.revanced.patches.youtube.player.overlaybuttons.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object ActivateDownloadButtonFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.name == "getAddToOfflineButtonState"
    }
)
