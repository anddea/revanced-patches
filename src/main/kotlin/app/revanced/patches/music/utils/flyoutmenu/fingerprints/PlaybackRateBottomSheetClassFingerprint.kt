package app.revanced.patches.music.utils.flyoutmenu.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.VarispeedUnavailableTitle
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlaybackRateBottomSheetClassFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = emptyList(),
    literalSupplier = { VarispeedUnavailableTitle }
)