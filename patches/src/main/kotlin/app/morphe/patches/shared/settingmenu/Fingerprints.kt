package app.morphe.patches.shared.settingmenu

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val findPreferenceFingerprint = legacyFingerprint(
    name = "findPreferenceFingerprint",
    returnType = "Landroidx/preference/Preference;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/CharSequence;"),
    strings = listOf("Key cannot be null"),
    customFingerprint = { method, _ ->
        method.definingClass == "Landroidx/preference/PreferenceGroup;"
    }
)

