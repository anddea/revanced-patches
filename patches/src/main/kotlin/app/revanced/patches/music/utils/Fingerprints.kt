package app.revanced.patches.music.utils

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val pendingIntentReceiverFingerprint = legacyFingerprint(
    name = "pendingIntentReceiverFingerprint",
    returnType = "V",
    strings = listOf("YTM Dislike", "YTM Next", "YTM Previous"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/PendingIntentReceiver;")
    }
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
