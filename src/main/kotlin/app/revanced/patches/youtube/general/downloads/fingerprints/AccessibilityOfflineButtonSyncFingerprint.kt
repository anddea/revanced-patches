package app.revanced.patches.youtube.general.downloads.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.general.downloads.fingerprints.AccessibilityOfflineButtonSyncFingerprint.ENDS_WITH_PARAMETER_LIST
import app.revanced.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.util.MethodUtil

internal object AccessibilityOfflineButtonSyncFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = custom@{ methodDef, _ ->
        if (!MethodUtil.isConstructor(methodDef)) {
            return@custom false
        }
        val parameterTypes = methodDef.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize < 6) {
            return@custom false
        }

        val endsWithMethodParameterList = parameterTypes.slice(parameterSize - 3..<parameterSize)
        parametersEqual(ENDS_WITH_PARAMETER_LIST, endsWithMethodParameterList)
    }
) {
    private val ENDS_WITH_PARAMETER_LIST = listOf(
        "Lcom/google/android/apps/youtube/app/offline/ui/OfflineArrowView;",
        "I",
        "Landroid/view/View${'$'}OnClickListener;"
    )
}