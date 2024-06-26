package app.revanced.patches.music.misc.backgroundplayback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getStartsWithStringInstructionIndex
import com.android.tools.smali.dexlib2.AccessFlags

internal object MusicBrowserServiceFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
    customFingerprint = custom@{ methodDef, _ ->
        if (!methodDef.definingClass.endsWith("/MusicBrowserService;"))
            return@custom false

        methodDef.getStartsWithStringInstructionIndex("MBS: Return empty root for client: %s") > 0
    }
)