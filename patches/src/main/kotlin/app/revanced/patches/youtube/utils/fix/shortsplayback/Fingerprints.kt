package app.revanced.patches.youtube.utils.fix.shortsplayback

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val shortsPlaybackFingerprint = legacyFingerprint(
    name = "shortsPlaybackFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(45387052L),
)
