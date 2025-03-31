package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val screenshotBannerContainerFingerprint = legacyFingerprint(
    name = "screenshotTakenBannerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf(
        "bannerContainer",
        "scope",
    )
)
