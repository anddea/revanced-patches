package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.patches.reddit.utils.resourceid.actionShare
import app.revanced.patches.reddit.utils.resourceid.screenShotShareBanner
import app.revanced.util.containsLiteralInstruction
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

/**
 * Reddit 2025.06.0 ~
 */
internal val screenshotTakenBannerComposableFingerprint = legacyFingerprint(
    name = "screenshotTakenBannerComposableFingerprint",
    returnType = "L",
    customFingerprint = { method, classDef ->
        method.containsLiteralInstruction(actionShare) &&
                method.containsLiteralInstruction(screenShotShareBanner) &&
                classDef.type.startsWith("Lcom/reddit/sharing/screenshot/composables/") &&
                method.name == "invoke"
    }
)

/**
 * ~ Reddit 2025.05.1
 */
internal val screenshotTakenBannerLambdaActionFingerprint = legacyFingerprint(
    name = "screenshotTakenBannerLambdaFingerprint",
    returnType = "V",
    parameters = listOf("Landroidx/compose/runtime/", "I"),
    customFingerprint = { method, _ ->
        method.containsLiteralInstruction(actionShare) &&
                method.name == "invoke"
    }
)

/**
 * ~ Reddit 2025.05.1
 */
internal val screenshotTakenBannerLambdaBannerFingerprint = legacyFingerprint(
    name = "screenshotTakenBannerLambdaFingerprint",
    returnType = "V",
    parameters = listOf("Landroidx/compose/runtime/", "I"),
    customFingerprint = { method, _ ->
        method.containsLiteralInstruction(screenShotShareBanner) &&
                method.name == "invoke"
    }
)


