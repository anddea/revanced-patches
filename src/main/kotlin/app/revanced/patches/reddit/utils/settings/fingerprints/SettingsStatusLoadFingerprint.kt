package app.revanced.patches.reddit.utils.settings.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.reddit.utils.integrations.Constants.INTEGRATIONS_PATH

internal object SettingsStatusLoadFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("$INTEGRATIONS_PATH/settings/SettingsStatus;") &&
                methodDef.name == "load"
    }
)