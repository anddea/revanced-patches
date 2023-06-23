package app.revanced.patches.reddit.utils.settings.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object SettingsStatusLoadFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("Lapp/revanced/reddit/settingsmenu/SettingsStatus;") &&
                methodDef.name == "load"
    }
)