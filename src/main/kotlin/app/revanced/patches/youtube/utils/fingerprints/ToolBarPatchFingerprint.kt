package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object ToolBarPatchFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lapp/revanced/integrations/patches/utils/ToolBarPatch;"
                && methodDef.name == "hookToolBar"
    }
)