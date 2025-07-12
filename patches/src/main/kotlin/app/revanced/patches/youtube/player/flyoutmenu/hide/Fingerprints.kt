package app.revanced.patches.youtube.player.flyoutmenu.hide

import app.revanced.patches.youtube.utils.indexOfAddHeaderViewInstruction
import app.revanced.patches.youtube.utils.resourceid.bottomSheetFooterText
import app.revanced.patches.youtube.utils.resourceid.subtitleMenuSettingsFooterInfo
import app.revanced.patches.youtube.utils.resourceid.videoQualityBottomSheet
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import app.revanced.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val advancedQualityBottomSheetFingerprint = legacyFingerprint(
    name = "advancedQualityBottomSheetFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L", "L"),
    customFingerprint = custom@{ method, _ ->
        if (!method.containsLiteralInstruction(videoQualityBottomSheet)) {
            return@custom false
        }
        if (indexOfAddHeaderViewInstruction(method) < 0) {
            return@custom false
        }
        val implementation = method.implementation
            ?: return@custom false

        implementation.instructions.elementAt(0).opcode == Opcode.IGET_OBJECT
    }
)

internal val captionsBottomSheetFingerprint = legacyFingerprint(
    name = "captionsBottomSheetFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(bottomSheetFooterText, subtitleMenuSettingsFooterInfo),
)

/**
 * This fingerprint is compatible with YouTube v18.39.xx+
 */
internal val pipModeConfigFingerprint = legacyFingerprint(
    name = "pipModeConfigFingerprint",
    literals = listOf(45427407L),
)

internal const val SLEEP_TIMER_CONSTRUCTOR_FEATURE_FLAG = 45640654L

internal val sleepTimerConstructorFingerprint = legacyFingerprint(
    name = "sleepTimerConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(SLEEP_TIMER_CONSTRUCTOR_FEATURE_FLAG),
)

internal const val SLEEP_TIMER_FEATURE_FLAG = 45630421L

internal val sleepTimerFingerprint = legacyFingerprint(
    name = "sleepTimerConstructorFingerprint",
    returnType = "Z",
    literals = listOf(SLEEP_TIMER_FEATURE_FLAG),
)

internal val videoQualityArrayFingerprint = legacyFingerprint(
    name = "videoQualityArrayFingerprint",
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
    customFingerprint = custom@{ method, _ ->
        val parameterTypes = method.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize != 3 && parameterSize != 4) {
            return@custom false
        }

        val startsWithMethodParameterList = parameterTypes.slice(0..0)
        val endsWithMethodParameterList = parameterTypes.slice(parameterSize - 2..<parameterSize)

        parametersEqual(
            VIDEO_QUALITY_ARRAY_STARTS_WITH_PARAMETER_LIST,
            startsWithMethodParameterList
        ) &&
                parametersEqual(
                    VIDEO_QUALITY_ARRAY_ENDS_WITH_PARAMETER_LIST,
                    endsWithMethodParameterList
                ) &&
                indexOfQualityLabelInstruction(method) >= 0
    }
)

private val VIDEO_QUALITY_ARRAY_STARTS_WITH_PARAMETER_LIST = listOf(
    "Ljava/util/List;"
)

private val VIDEO_QUALITY_ARRAY_ENDS_WITH_PARAMETER_LIST = listOf(
    "Ljava/lang/String;",
    "L"
)

internal fun indexOfQualityLabelInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.returnType == "Ljava/lang/String;" &&
                reference.parameterTypes.size == 0 &&
                reference.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;"
    }
