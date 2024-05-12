package app.revanced.patches.music.misc.minimizedplayback.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object DataSavingSettingsFragmentFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
    strings = listOf("pref_key_dont_play_nma_video"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/DataSavingSettingsFragment;")
                && methodDef.name == "onCreatePreferences"
    }
)