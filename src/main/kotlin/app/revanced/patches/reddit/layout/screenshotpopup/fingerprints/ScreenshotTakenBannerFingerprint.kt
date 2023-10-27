package app.revanced.patches.reddit.layout.screenshotpopup.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch.ScreenShotShareBanner
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object ScreenshotTakenBannerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, classDef ->
        methodDef.isWideLiteralExists(ScreenShotShareBanner)
                && classDef.sourceFile == "ScreenshotTakenBanner.kt"
    }
)