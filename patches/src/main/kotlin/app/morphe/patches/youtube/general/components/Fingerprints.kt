/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.patches.youtube.utils.resourceid.accountSwitcherAccessibility
import app.morphe.patches.youtube.utils.resourceid.compactLink
import app.morphe.patches.youtube.utils.resourceid.compactListItem
import app.morphe.patches.youtube.utils.resourceid.editSettingsAction
import app.morphe.patches.youtube.utils.resourceid.fab
import app.morphe.patches.youtube.utils.resourceid.toolTipContentView
import app.morphe.patches.youtube.utils.resourceid.ytCallToAction
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val accountListFingerprint = legacyFingerprint(
    name = "accountListFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    literals = listOf(ytCallToAction),
)

internal val accountListParentFingerprint = legacyFingerprint(
    name = "accountListParentFingerprint",
    literals = listOf(compactListItem),
)

internal val accountMenuFingerprint = legacyFingerprint(
    name = "accountMenuFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.IGET,
        Opcode.AND_INT_LIT16
    )
)

internal val accountMenuParentFingerprint = legacyFingerprint(
    name = "accountMenuParentFingerprint",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literals = listOf(compactLink),
)

internal val accountSwitcherAccessibilityLabelFingerprint = legacyFingerprint(
    name = "accountSwitcherAccessibilityLabelFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    literals = listOf(accountSwitcherAccessibility),
)

internal val appBlockingCheckResultToStringFingerprint = legacyFingerprint(
    name = "appBlockingCheckResultToStringFingerprint",
    returnType = "Ljava/lang/String;",
    strings = listOf("AppBlockingCheckResult{intent=")
)

internal val floatingMicrophoneFingerprint = legacyFingerprint(
    name = "floatingMicrophoneFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
    ),
    literals = listOf(fab),
)

internal val pipNotificationFingerprint = legacyFingerprint(
    name = "pipNotificationFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(editSettingsAction),
)

internal val tooltipContentFullscreenFingerprint = legacyFingerprint(
    name = "tooltipContentFullscreenFingerprint",
    returnType = "V",
    literals = listOf(45384061L),
)

internal val tooltipContentViewFingerprint = legacyFingerprint(
    name = "tooltipContentViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(toolTipContentView),
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

internal object CommentReplyPaddingFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45752241)
    )
)

internal object IncognitoSearchPaddingFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45724388)
    )
)
