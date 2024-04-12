package app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object MainActivityOnBackPressedFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf(),
    customFingerprint = { methodDef, _ ->
        (methodDef.definingClass.endsWith("MainActivity;") ||
                // Old versions of YouTube called this class "WatchWhileActivity" instead.
                methodDef.definingClass.endsWith("WatchWhileActivity;"))
                && methodDef.name == "onBackPressed"
    }
)
