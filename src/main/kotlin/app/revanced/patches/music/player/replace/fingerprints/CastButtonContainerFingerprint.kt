package app.revanced.patches.music.player.replace.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PlayerCastMediaRouteButton
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object CastButtonContainerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { PlayerCastMediaRouteButton }
)
