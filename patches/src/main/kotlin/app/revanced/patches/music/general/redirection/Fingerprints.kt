package app.revanced.patches.music.general.redirection

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val dislikeButtonOnClickListenerFingerprint = legacyFingerprint(
    name = "dislikeButtonOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    literals = listOf(53465L),
    customFingerprint = { method, _ ->
        method.name == "onClick"
    }
)

