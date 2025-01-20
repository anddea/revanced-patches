package app.revanced.patches.shared.spoof.useragent

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

const val CLIENT_PACKAGE_NAME = "cbr"

internal val apiStatsFingerprint = legacyFingerprint(
    name = "apiStatsFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    strings = listOf(CLIENT_PACKAGE_NAME),
)
