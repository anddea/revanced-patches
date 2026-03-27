package app.morphe.patches.youtube.player.flyoutmenu.hide

import app.morphe.patches.youtube.utils.YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
import app.morphe.patches.youtube.utils.indexOfAddHeaderViewInstruction
import app.morphe.patches.youtube.utils.resourceid.bottomSheetFooterText
import app.morphe.patches.youtube.utils.resourceid.subtitleMenuSettingsFooterInfo
import app.morphe.patches.youtube.utils.resourceid.videoQualityBottomSheet
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

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

internal val currentVideoFormatConstructorFingerprint = legacyFingerprint(
    name = "currentVideoFormatConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { method, _ ->
        indexOfVideoQualitiesInstruction(method) >= 0
    },
)

internal fun indexOfVideoQualitiesInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.IPUT_OBJECT &&
                getReference<FieldReference>()?.type == "[$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE"
    }

internal val currentVideoFormatToStringFingerprint = legacyFingerprint(
    name = "currentVideoFormatToStringFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("currentVideoFormat="),
    customFingerprint = { method, _ ->
        method.name == "toString"
    },
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
