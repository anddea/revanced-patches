/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.fullscreen

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import app.morphe.patches.youtube.utils.resourceid.appRelatedEndScreenResults
import app.morphe.patches.youtube.utils.resourceid.fullScreenEngagementPanel
import app.morphe.patches.youtube.utils.resourceid.playerVideoTitleView
import app.morphe.patches.youtube.utils.resourceid.quickActionsElementContainer
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object BroadcastReceiverFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    strings = listOf(
        "android.intent.action.SCREEN_ON",
        "android.intent.action.SCREEN_OFF",
        "android.intent.action.BATTERY_CHANGED"
    ),
    custom = { _, classDef ->
        classDef.superclass == "Landroid/content/BroadcastReceiver;"
    }
)

internal object FullScreenEngagementPanelFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        literal(fullScreenEngagementPanel),
    ),
)

/**
 * This fingerprint is compatible with YouTube v18.42.41+
 */
internal object LandScapeModeConfigFingerprint : Fingerprint(
    returnType = "Z",
    filters = listOf(
        literal(45446428L),
    ),
)

internal object PlayerTitleViewFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        literal(playerVideoTitleView),
    ),
)

internal object QuickActionsElementSyntheticFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        literal(quickActionsElementContainer),
    ),
    custom = { _, classDef ->
        AccessFlags.SYNTHETIC.isSet(classDef.accessFlags)
    }
)

internal object RelatedEndScreenResultsFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        literal(appRelatedEndScreenResults),
    ),
)

internal object YouTubePlayerOverlaysLayoutConstructorFingerprint : Fingerprint(
    definingClass = "/YouTubePlayerOverlaysLayout;",
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;"),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)

internal object YouTubePlayerViewOnLayoutFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/player/YouTubePlayerViewNotForReflection;",
    name = "onLayout",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "I", "I", "I", "I"),
    filters = listOf(
        opcode(Opcode.RETURN_VOID)
    )
)
