package app.morphe.patches.youtube.utils.bottomsheet

import app.morphe.patches.youtube.utils.resourceid.designBottomSheet
import app.morphe.util.fingerprint.legacyFingerprint

internal val bottomSheetBehaviorFingerprint = legacyFingerprint(
    name = "bottomSheetBehaviorFingerprint",
    returnType = "V",
    literals = listOf(designBottomSheet),
)
