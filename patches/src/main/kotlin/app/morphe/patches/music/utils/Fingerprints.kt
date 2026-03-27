package app.morphe.patches.music.utils

import app.morphe.patches.music.utils.resourceid.varispeedUnavailableTitle
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val ACTION_BAR_POSITION_FEATURE_FLAG = 45658717L

internal val actionBarPositionFeatureFlagFingerprint = legacyFingerprint(
    name = "actionBarPositionFeatureFlagFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(ACTION_BAR_POSITION_FEATURE_FLAG)
)

internal val pendingIntentReceiverFingerprint = legacyFingerprint(
    name = "pendingIntentReceiverFingerprint",
    returnType = "V",
    strings = listOf("YTM Dislike", "YTM Next", "YTM Previous"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/PendingIntentReceiver;")
    }
)

internal val playbackSpeedBottomSheetFingerprint = legacyFingerprint(
    name = "playbackSpeedBottomSheetFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT")
)

internal val playbackRateBottomSheetClassFingerprint = legacyFingerprint(
    name = "playbackRateBottomSheetClassFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(varispeedUnavailableTitle)
)

internal val playbackSpeedFingerprint = legacyFingerprint(
    name = "playbackSpeedFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_VIRTUAL
    )
)

internal val playbackSpeedParentFingerprint = legacyFingerprint(
    name = "playbackSpeedParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("BT metadata: %s, %s, %s")
)
