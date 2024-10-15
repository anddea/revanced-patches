package app.revanced.patches.music.general.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object PreferenceScreenFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/google/android/apps/youtube/music/settings/fragment/SettingsHeadersFragment;" &&
                methodDef.name == "onCreatePreferences"
    }
)