package app.morphe.patches.shared.spoof.useragent

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

const val CLIENT_PACKAGE_NAME = "cbr"

internal val apiStatsFingerprint = legacyFingerprint(
    name = "apiStatsFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    strings = listOf(CLIENT_PACKAGE_NAME),
)
