package app.revanced.patches.youtube.layout.pipnotification.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.EditSettingsAction
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object PiPNotificationFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(EditSettingsAction) }
)