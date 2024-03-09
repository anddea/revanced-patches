package app.revanced.patches.youtube.player.playerbuttonbg.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object PlayerPatchFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lapp/revanced/integrations/youtube/patches/player/PlayerPatch;"
            && methodDef.name == "hidePlayerButton"
    }
)
