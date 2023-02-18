package app.revanced.patches.youtube.layout.player.playerbuttonbg.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object HidePlayerButtonFingerprint : MethodFingerprint (
    customFingerprint = {
        it.definingClass == "Lapp/revanced/integrations/patches/layout/PlayerLayoutPatch;"
                && it.name == "hidePlayerButton"
    }
)