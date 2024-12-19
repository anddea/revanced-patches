package app.revanced.patches.youtube.utils.fix.cairo

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Added in YouTube v19.04.38
 *
 * When this value is TRUE, Cairo Fragment is used.
 * In this case, some of patches may be broken, so set this value to FALSE.
 */
internal val carioFragmentConfigFingerprint = legacyFingerprint(
    name = "carioFragmentConfigFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(45532100L),
)
