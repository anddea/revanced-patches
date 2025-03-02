package app.revanced.patches.youtube.utils.bottomsheet

import app.revanced.patches.youtube.utils.resourceid.designBottomSheet
import app.revanced.util.fingerprint.legacyFingerprint

internal val bottomSheetBehaviorFingerprint = legacyFingerprint(
    name = "bottomSheetBehaviorFingerprint",
    returnType = "V",
    literals = listOf(designBottomSheet),
)
