package app.revanced.patches.youtube.player.flyoutmenu.hide.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.player.flyoutmenu.hide.fingerprints.VideoQualityArrayFingerprint.ENDS_WITH_PARAMETER_LIST
import app.revanced.patches.youtube.player.flyoutmenu.hide.fingerprints.VideoQualityArrayFingerprint.STARTS_WITH_PARAMETER_LIST
import app.revanced.patches.youtube.player.flyoutmenu.hide.fingerprints.VideoQualityArrayFingerprint.indexOfQualityLabelInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object VideoQualityArrayFingerprint : MethodFingerprint(
    returnType = "[Lcom/google/android/libraries/youtube/innertube/model/media/VideoQuality;",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    // 18.29 and earlier parameters are:
    // "Ljava/util/List;",
    // "Ljava/lang/String;"
    // "L"

    // 18.31+ parameters are:
    // "Ljava/util/List;",
    // "Ljava/util/Collection;",
    // "Ljava/lang/String;"
    // "L"
    customFingerprint = custom@{ methodDef, _ ->
        val parameterTypes = methodDef.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize != 3 && parameterSize != 4) {
            return@custom false
        }

        val startsWithMethodParameterList = parameterTypes.slice(0..0)
        val endsWithMethodParameterList = parameterTypes.slice(parameterSize - 2..< parameterSize)

        parametersEqual(STARTS_WITH_PARAMETER_LIST, startsWithMethodParameterList) &&
                parametersEqual(ENDS_WITH_PARAMETER_LIST, endsWithMethodParameterList) &&
                indexOfQualityLabelInstruction(methodDef) >= 0
    }
) {
    private val STARTS_WITH_PARAMETER_LIST = listOf(
        "Ljava/util/List;"
    )
    private val ENDS_WITH_PARAMETER_LIST = listOf(
        "Ljava/lang/String;",
        "L"
    )

    fun indexOfQualityLabelInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_VIRTUAL &&
                    reference?.returnType == "Ljava/lang/String;" &&
                    reference.parameterTypes.size == 0 &&
                    reference.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;"
        }
}