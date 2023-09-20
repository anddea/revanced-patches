package app.revanced.patches.music.utils.overridequality.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object VideoQualityPatchFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    parameters = listOf("I"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lapp/revanced/music/patches/video/VideoQualityPatch;"
                && methodDef.name == "overrideQuality"
    }
)