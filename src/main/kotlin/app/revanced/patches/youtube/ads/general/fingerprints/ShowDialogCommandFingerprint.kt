package app.revanced.patches.youtube.ads.general.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.SlidingDialogAnimation
import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object ShowDialogCommandFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("[B", "L"),
    literalSupplier = { SlidingDialogAnimation }
)