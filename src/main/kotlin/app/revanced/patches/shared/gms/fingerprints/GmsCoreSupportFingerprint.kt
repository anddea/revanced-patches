package app.revanced.patches.shared.gms.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object GmsCoreSupportFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/GmsCoreSupport;")
                && methodDef.name == "getGmsCoreVendorGroupId"
    },
)
