package app.revanced.patches.youtube.player.fullscreen

import app.revanced.patches.youtube.utils.resourceid.appRelatedEndScreenResults
import app.revanced.patches.youtube.utils.resourceid.fullScreenEngagementPanel
import app.revanced.patches.youtube.utils.resourceid.playerVideoTitleView
import app.revanced.patches.youtube.utils.resourceid.quickActionsElementContainer
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.util.MethodUtil

internal val broadcastReceiverFingerprint = legacyFingerprint(
    name = "broadcastReceiverFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    strings = listOf(
        "android.intent.action.SCREEN_ON",
        "android.intent.action.SCREEN_OFF",
        "android.intent.action.BATTERY_CHANGED"
    ),
    customFingerprint = { _, classDef ->
        classDef.superclass == "Landroid/content/BroadcastReceiver;"
    }
)

internal val engagementPanelFingerprint = legacyFingerprint(
    name = "engagementPanelFingerprint",
    returnType = "L",
    parameters = listOf("L"),
    literals = listOf(fullScreenEngagementPanel),
)

/**
 * This fingerprint is compatible with YouTube v18.42.41+
 */
internal val landScapeModeConfigFingerprint = legacyFingerprint(
    name = "landScapeModeConfigFingerprint",
    returnType = "Z",
    literals = listOf(45446428L),
)

internal val playerTitleViewFingerprint = legacyFingerprint(
    name = "playerTitleViewFingerprint",
    returnType = "V",
    literals = listOf(playerVideoTitleView),
)

internal val quickActionsElementSyntheticFingerprint = legacyFingerprint(
    name = "quickActionsElementSyntheticFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    literals = listOf(quickActionsElementContainer),
    customFingerprint = { _, classDef ->
        AccessFlags.SYNTHETIC.isSet(classDef.accessFlags)
    }
)

internal val relatedEndScreenResultsFingerprint = legacyFingerprint(
    name = "relatedEndScreenResultsFingerprint",
    returnType = "V",
    literals = listOf(appRelatedEndScreenResults),
)

