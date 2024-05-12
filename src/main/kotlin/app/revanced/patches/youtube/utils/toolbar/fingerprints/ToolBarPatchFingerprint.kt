package app.revanced.patches.youtube.utils.toolbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.AccessFlags

internal object ToolBarPatchFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "$UTILS_PATH/ToolBarPatch;"
                && methodDef.name == "hookToolBar"
    }
)