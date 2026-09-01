package app.morphe.patches.shared.settingmenu

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * PreferenceGroup.findPreference(CharSequence). The null-key guard distinguishes this lookup
 * from other PreferenceGroup methods with a Preference return type.
 */
internal object PreferenceGroupFindPreferenceFingerprint : Fingerprint(
    definingClass = "Landroidx/preference/PreferenceGroup;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/CharSequence;"),
    returnType = "Landroidx/preference/Preference;",
    strings = listOf("Key cannot be null"),
)
