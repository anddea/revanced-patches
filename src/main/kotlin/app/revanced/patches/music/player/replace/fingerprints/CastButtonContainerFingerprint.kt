package app.revanced.patches.music.player.replace.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.PlayerCastMediaRouteButton
import app.revanced.util.bytecode.isWideLiteralExists

object CastButtonContainerFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(PlayerCastMediaRouteButton)
                && methodDef.definingClass.endsWith("/MusicActivity;")
    }
)
