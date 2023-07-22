package app.revanced.patches.youtube.utils.litho.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object LithoFilterFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.STATIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lapp/revanced/integrations/patches/ads/LithoFilterPatch;"
    }
)