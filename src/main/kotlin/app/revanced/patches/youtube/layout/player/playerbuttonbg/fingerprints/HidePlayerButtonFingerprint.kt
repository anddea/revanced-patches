package app.revanced.patches.youtube.layout.player.playerbuttonbg.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object HidePlayerButtonFingerprint : MethodFingerprint (
    customFingerprint = { it, _ ->
        it.definingClass == "Lapp/revanced/integrations/patches/layout/PlayerPatch;"
                && it.name == "hidePlayerButton"
    }
)