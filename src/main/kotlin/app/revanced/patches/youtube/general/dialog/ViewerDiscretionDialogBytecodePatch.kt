package app.revanced.patches.youtube.general.dialog

import app.revanced.patches.shared.dialog.BaseViewerDiscretionDialogPatch
import app.revanced.patches.youtube.general.dialog.fingerprints.AgeVerifiedFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR

object ViewerDiscretionDialogBytecodePatch : BaseViewerDiscretionDialogPatch(
    GENERAL_CLASS_DESCRIPTOR,
    setOf(AgeVerifiedFingerprint)
)
