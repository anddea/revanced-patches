package app.morphe.patches.youtube.utils.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patches.youtube.utils.resourceid.appearance
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val licenseActivityOnCreateFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    custom = { method, classDef ->
        classDef.endsWith("LicenseActivity;") && method.name == "onCreate"
    }
)

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

internal val baseHostActivityOnCreateFingerprint = legacyFingerprint(
    name = "baseHostActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.endsWith("/BaseHostActivity;") && method.name == "onCreate"
    }
)

internal val youtubeHostActivityOnCreateFingerprint = legacyFingerprint(
    name = "youtubeHostActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.endsWith("/YouTubeHostActivity;") && method.name == "onCreate"
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
