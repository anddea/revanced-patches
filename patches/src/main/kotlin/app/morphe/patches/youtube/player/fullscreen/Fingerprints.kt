package app.morphe.patches.youtube.player.fullscreen

import app.morphe.patches.youtube.utils.resourceid.appRelatedEndScreenResults
import app.morphe.patches.youtube.utils.resourceid.fullScreenEngagementPanel
import app.morphe.patches.youtube.utils.resourceid.playerVideoTitleView
import app.morphe.patches.youtube.utils.resourceid.quickActionsElementContainer
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

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

internal val fullScreenEngagementPanelFingerprint = legacyFingerprint(
    name = "fullScreenEngagementPanelFingerprint",
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

