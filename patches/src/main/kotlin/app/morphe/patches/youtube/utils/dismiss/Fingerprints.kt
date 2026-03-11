package app.morphe.patches.youtube.utils.dismiss

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal const val DISMISS_PLAYER_LITERAL = 34699L
internal const val DISMISS_PLAYER_2031_LITERAL = 131070L

internal val dismissPlayerOnClickListenerFingerprint = legacyFingerprint(
    name = "dismissPlayerOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(DISMISS_PLAYER_LITERAL),
    customFingerprint = { method, _ ->
        method.name == "onClick"
    }
)

internal val dismissPlayerOnClickListener2031Fingerprint = legacyFingerprint(
    name = "dismissPlayerOnClickListener2031Fingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(DISMISS_PLAYER_2031_LITERAL),
    customFingerprint = { method, _ ->
        method.name == "onClick"
    }
)