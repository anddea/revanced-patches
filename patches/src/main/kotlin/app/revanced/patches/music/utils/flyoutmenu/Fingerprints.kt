package app.revanced.patches.music.utils.flyoutmenu

import app.revanced.patches.music.utils.resourceid.varispeedUnavailableTitle
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val playbackRateBottomSheetClassFingerprint = legacyFingerprint(
    name = "playbackRateBottomSheetClassFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(varispeedUnavailableTitle)
)
