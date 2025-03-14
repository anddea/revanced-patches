package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.patches.reddit.utils.resourceid.screenShotShareBanner
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode

/**
 * Reddit 2025.06.0 ~
 */
internal val screenshotTakenBannerFingerprint = legacyFingerprint(
    name = "screenshotTakenBannerFingerprint",
    returnType = "L",
    opcodes = listOf(
        Opcode.CONST_4,
        Opcode.IF_NE,
    ),
    customFingerprint = { method, classDef ->
        method.containsLiteralInstruction(screenShotShareBanner) &&
        classDef.type.startsWith("Lcom/reddit/sharing/screenshot/composables/") &&
                method.name == "invoke"
    }
)

/**
 * ~ Reddit 2025.05.1
 */
internal val screenshotTakenBannerLegacyFingerprint = legacyFingerprint(
    name = "screenshotTakenBannerLegacyFingerprint",
    returnType = "V",
    parameters = listOf("Landroidx/compose/runtime/", "I"),
    customFingerprint = { method, classDef ->
        classDef.type.endsWith("\$ScreenshotTakenBannerKt\$lambda-1\$1;") &&
                method.name == "invoke"
    }
)
