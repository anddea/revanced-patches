package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.patches.reddit.utils.resourceid.screenShotShareBanner
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val screenshotTakenBannerFingerprint = legacyFingerprint(
    name = "screenshotTakenBannerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(screenShotShareBanner),
    customFingerprint = { _, classDef ->
        classDef.sourceFile == "ScreenshotTakenBanner.kt"
    }
)
