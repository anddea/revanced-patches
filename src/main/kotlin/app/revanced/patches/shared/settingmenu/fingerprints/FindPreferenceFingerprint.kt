package app.revanced.patches.shared.settingmenu.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object FindPreferenceFingerprint : MethodFingerprint(
    returnType = "Landroidx/preference/Preference;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/CharSequence;"),
    strings = listOf("Key cannot be null"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Landroidx/preference/PreferenceGroup;"
    }
)