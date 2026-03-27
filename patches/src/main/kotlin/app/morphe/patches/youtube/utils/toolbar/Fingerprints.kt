package app.morphe.patches.youtube.utils.toolbar

import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val toolBarPatchFingerprint = legacyFingerprint(
    name = "toolBarPatchFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    customFingerprint = { method, _ ->
        method.definingClass == "$UTILS_PATH/ToolBarPatch;"
                && method.name == "hookToolBar"
    }
)


