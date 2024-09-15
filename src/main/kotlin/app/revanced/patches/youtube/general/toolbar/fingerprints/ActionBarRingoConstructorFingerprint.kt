package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.util.MethodUtil

internal object ActionBarRingoConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("default"),
    customFingerprint = custom@{ methodDef, _ ->
        if (!MethodUtil.isConstructor(methodDef)) {
            return@custom false
        }

        val parameterTypes = methodDef.parameterTypes
        parameterTypes.size >= 5 && parameterTypes[0] == "Landroid/content/Context;"
    }
)