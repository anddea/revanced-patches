package app.revanced.patches.music.ads.general.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.SlidingDialogAnimation
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object ShowDialogCommandFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("[B", "L"),
    literalSupplier = { SlidingDialogAnimation }
)