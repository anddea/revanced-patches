package app.revanced.patches.music.layout.sharebuttonhook.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MusicSettingsFingerprint : MethodFingerprint(
    customFingerprint = {
        it.definingClass.endsWith("MusicSettings;") && it.name == "getDownloaderPackageName"
    }
)
