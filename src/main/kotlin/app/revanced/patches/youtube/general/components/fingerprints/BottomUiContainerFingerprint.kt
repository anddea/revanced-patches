package app.revanced.patches.youtube.general.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object BottomUiContainerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/BottomUiContainer;")
    }
)