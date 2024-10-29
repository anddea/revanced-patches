package app.revanced.patches.music.misc.backgroundplayback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object MusicBrowserServiceFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
    strings = listOf("android.service.media.extra.RECENT"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/MusicBrowserService;")
    },
)