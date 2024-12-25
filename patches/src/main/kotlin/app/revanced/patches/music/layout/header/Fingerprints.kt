package app.revanced.patches.music.layout.header

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val headerSwitchConfigFingerprint = legacyFingerprint(
    name = "headerSwitchConfigFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(45617851L)
)
