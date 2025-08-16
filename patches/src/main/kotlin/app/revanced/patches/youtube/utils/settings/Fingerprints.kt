package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.fingerprint
import app.revanced.patches.youtube.utils.resourceid.appearance
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val licenseActivityOnCreateFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters("L")
    custom { method, classDef ->
        classDef.endsWith("LicenseActivity;") && method.name == "onCreate"
    }
}

internal val settingsFragmentStylePrimaryFingerprint = legacyFingerprint(
    name = "settingsFragmentStylePrimaryFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/util/List;",
        "Landroidx/preference/Preference;",
        "Lj${'$'}/util/Optional;",
        "Lj${'$'}/util/Optional;",
    ),
)

internal val settingsFragmentStyleSecondaryFingerprint = legacyFingerprint(
    name = "settingsFragmentStyleSecondaryFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(
        "Ljava/util/List;",
        "Landroidx/preference/Preference;",
    ),
)

internal val themeSetterSystemFingerprint = legacyFingerprint(
    name = "themeSetterSystemFingerprint",
    returnType = "L",
    literals = listOf(appearance),
)

internal val settingsHostActivityOnCreateFingerprint = legacyFingerprint(
    name = "settingsHostActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.endsWith("/ReVancedSettingsHostActivity;") && method.name == "onCreate"
    }
)

internal val licenseMenuActivityOnCreateFingerprint = legacyFingerprint(
    name = "licenseMenuActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.endsWith("/LicenseMenuActivity;") && method.name == "onCreate"
    }
)

internal val proxyBillingActivityV2OnCreateFingerprint = legacyFingerprint(
    name = "proxyBillingActivityV2OnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.endsWith("/ProxyBillingActivityV2;") && method.name == "onCreate"
    }
)
