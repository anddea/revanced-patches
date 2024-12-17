package app.revanced.patches.music.general.redirection

import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val dislikeButtonOnClickListenerFingerprint = legacyFingerprint(
    name = "dislikeButtonOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    customFingerprint = { method, _ ->
        method.name == "onClick" &&
                (method.containsLiteralInstruction(53465L) || method.containsLiteralInstruction(98173L))
    }
)

