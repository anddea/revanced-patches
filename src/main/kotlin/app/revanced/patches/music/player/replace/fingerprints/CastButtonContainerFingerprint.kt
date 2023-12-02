package app.revanced.patches.music.player.replace.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PlayerCastMediaRouteButton
import app.revanced.util.bytecode.isWideLiteralExists

object CastButtonContainerFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(PlayerCastMediaRouteButton)
    }
)
