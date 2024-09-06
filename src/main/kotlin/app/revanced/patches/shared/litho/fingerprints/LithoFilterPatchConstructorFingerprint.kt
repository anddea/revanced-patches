package app.revanced.patches.shared.litho.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.shared.integrations.Constants.COMPONENTS_PATH
import com.android.tools.smali.dexlib2.AccessFlags

internal object LithoFilterPatchConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "$COMPONENTS_PATH/LithoFilterPatch;"
    }
)