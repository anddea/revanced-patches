package app.morphe.patches.reddit.utils.settings

import app.morphe.patches.reddit.utils.extension.Constants
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val acknowledgementsLabelBuilderFingerprint = legacyFingerprint(
    name = "acknowledgementsLabelBuilderFingerprint",
    returnType = "Z",
    parameters = listOf("Landroidx/preference/Preference;"),
    strings = listOf("onboardingAnalytics"),
    customFingerprint = { method, _ ->
        method.definingClass.startsWith("Lcom/reddit/screen/settings/preferences/")
    }
)

internal val ossLicensesMenuActivityOnCreateFingerprint = legacyFingerprint(
    name = "ossLicensesMenuActivityOnCreateFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
        Opcode.INVOKE_STATIC
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/OssLicensesMenuActivity;") &&
                method.name == "onCreate"
    }
)

internal val redditInternalFeaturesFingerprint = legacyFingerprint(
    name = "redditInternalFeaturesFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("RELEASE"),
    customFingerprint = { methodDef, _ ->
        !methodDef.definingClass.startsWith("Lcom/")
    }
)

internal val settingsStatusLoadFingerprint = legacyFingerprint(
    name = "settingsStatusLoadFingerprint",
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("${Constants.EXTENSION_PATH}/settings/SettingsStatus;") &&
                method.name == "load"
    }
)