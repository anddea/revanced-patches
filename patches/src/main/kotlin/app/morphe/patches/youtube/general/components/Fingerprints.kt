/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 *
 * Modified by lavinhoque33, 2026-10-08.
 */

package app.morphe.patches.youtube.general.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.OpcodesFilter.Companion.opcodesToFilters
import app.morphe.patcher.checkCast
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.patches.youtube.utils.resourceid.accountSwitcherAccessibility
import app.morphe.patches.youtube.utils.resourceid.compactLink
import app.morphe.patches.youtube.utils.resourceid.compactListItem
import app.morphe.patches.youtube.utils.resourceid.editSettingsAction
import app.morphe.patches.youtube.utils.resourceid.toolTipContentView
import app.morphe.patches.youtube.utils.resourceid.ytCallToAction
import app.morphe.util.containsLiteralInstruction
import app.morphe.patcher.string
import app.morphe.patcher.StringComparisonType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object accountListFingerprint : Fingerprint(
    classFingerprint = accountListParentFingerprint,
    returnType = "V",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL, AccessFlags.SYNTHETIC),
    custom = { method, _ -> method.containsLiteralInstruction(ytCallToAction) },
)

internal object accountListParentFingerprint : Fingerprint(
    custom = { method, _ -> method.containsLiteralInstruction(compactListItem) },
)

internal object accountMenuFingerprint : Fingerprint(
    classFingerprint = accountMenuParentFingerprint,
    returnType = "V",
    filters = opcodesToFilters(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.IGET,
        Opcode.AND_INT_LIT16
    )
)

internal object accountMenuParentFingerprint : Fingerprint(
    filters = opcodesToFilters(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    custom = { method, _ -> method.containsLiteralInstruction(compactLink) },
)

internal object accountSwitcherAccessibilityLabelFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    custom = { method, _ -> method.containsLiteralInstruction(accountSwitcherAccessibility) },
)

internal object appBlockingCheckResultToStringFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf(string("AppBlockingCheckResult{intent=", StringComparisonType.CONTAINS))
)


internal object pipNotificationFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    custom = { method, _ -> method.containsLiteralInstruction(editSettingsAction) },
)

internal object tooltipContentFullscreenFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, _ -> method.containsLiteralInstruction(45384061L) },
)

internal object tooltipContentViewFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L"),
    custom = { method, _ -> method.containsLiteralInstruction(toolTipContentView) },
)

internal object SyncButtonFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "sync_button"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "inflate",
            returnType = "Landroid/view/View;",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)

/**
 * Makes the engagement panels extend behind the navigation bar, which hides the bottom
 * of the panels that do not add the bottom padding themselves, such as the live chat.
 */
internal object EngagementPanelEdgeToEdgeFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45744676)
    )
)

/**
 * 21.11+
 *
 * Resolves using the method found in [ShowFloatingMicrophoneButtonParentFingerprint]
 */
internal object ShowFloatingMicrophoneButtonFingerprint : Fingerprint(
    classFingerprint = ShowFloatingMicrophoneButtonParentFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "Lcom/google/android/libraries/quantum/fab/FloatingActionButton;", "Landroid/view/ViewStub;"),
    filters = opcodesToFilters(Opcode.IGET_BOOLEAN)
)

/**
 * 21.25+
 *
 * Same as [ShowFloatingMicrophoneButtonFingerprint], but the button is inflated later from a ViewStub.
 * Used instead of [ShowFloatingMicrophoneButtonFingerprint] with some server layout experiments.
 */
internal object ShowFloatingMicrophoneButtonStubFingerprint : Fingerprint(
    classFingerprint = ShowFloatingMicrophoneButtonParentFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "Landroid/view/ViewStub;", "Landroid/view/ViewStub;"),
    filters = opcodesToFilters(Opcode.IGET_BOOLEAN)
)

/**
 * 21.11+
 */
internal object ShowFloatingMicrophoneButtonParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z"),
    strings = listOf("Current FAB View Wrapper does not support this operation. Text: "),
    custom = { _, classDef ->
        !classDef.interfaces.contains($$"Landroid/view/View$OnClickListener;")
    }
)

/**
 * ~ 21.10
 */
internal object ShowFloatingMicrophoneButtonLegacyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "fab"),
        checkCast("/FloatingActionButton;", location = MatchAfterWithin(10)),
        opcode(Opcode.IGET_BOOLEAN, location = MatchAfterWithin(15))
    )
)
