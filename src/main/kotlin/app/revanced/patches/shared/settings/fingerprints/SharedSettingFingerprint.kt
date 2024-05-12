package app.revanced.patches.shared.settings.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.shared.integrations.Constants.INTEGRATIONS_SETTING_CLASS_DESCRIPTOR

internal object SharedSettingFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == INTEGRATIONS_SETTING_CLASS_DESCRIPTOR
                && methodDef.name == "<clinit>"
    }
)