package app.revanced.patches.music.utils.overridespeed.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object PlaybackSpeedPatchFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("F"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lapp/revanced/music/patches/misc/PlaybackSpeedPatch;"
                && methodDef.name == "overrideSpeed"
    }
)