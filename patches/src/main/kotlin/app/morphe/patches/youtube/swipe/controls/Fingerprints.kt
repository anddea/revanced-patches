package app.morphe.patches.youtube.swipe.controls

import app.morphe.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.morphe.patches.youtube.utils.resourceid.autoNavScrollCancelPadding
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val hdrBrightnessFingerprint = legacyFingerprint(
    name = "hdrBrightnessFingerprint",
    returnType = "V",
    strings = listOf("mediaViewambientBrightnessSensor")
)

internal val swipeControlsHostActivityFingerprint = legacyFingerprint(
    name = "swipeControlsHostActivityFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = emptyList(),
    customFingerprint = { _, classDef ->
        classDef.type == "$EXTENSION_PATH/swipecontrols/SwipeControlsHostActivity;"
    }
)

internal const val SWIPE_TO_SWITCH_VIDEO_FEATURE_FLAG = 45631116L

/**
 * This fingerprint is compatible with YouTube v19.19.39+
 */
internal val swipeToSwitchVideoFingerprint = legacyFingerprint(
    name = "swipeToSwitchVideoFingerprint",
    returnType = "V",
    literals = listOf(SWIPE_TO_SWITCH_VIDEO_FEATURE_FLAG),
)

internal const val WATCH_PANEL_GESTURES_PRIMARY_FEATURE_FLAG = 45372793L

/**
 * This fingerprint is compatible with YouTube v18.29.38 ~ v19.34.42
 */
internal val watchPanelGesturesFingerprint = legacyFingerprint(
    name = "watchPanelGesturesFingerprint",
    returnType = "V",
    literals = listOf(WATCH_PANEL_GESTURES_PRIMARY_FEATURE_FLAG),
)

internal val watchPanelGesturesAlternativeFingerprint = legacyFingerprint(
    name = "watchPanelGesturesAlternativeFingerprint",
    literals = listOf(autoNavScrollCancelPadding),
)

internal const val WATCH_PANEL_GESTURES_SECONDARY_FEATURE_FLAG = 45619395L

/**
 * Watch panel gestures in channel bar
 * This fingerprint is compatible with YouTube v19.15.36 ~
 */
internal val watchPanelGesturesChannelBarFingerprint = legacyFingerprint(
    name = "watchPanelGesturesChannelBarFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/MotionEvent;"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/NextGenWatchLayout;") &&
                method.name == "onInterceptTouchEvent" &&
                method.containsLiteralInstruction(WATCH_PANEL_GESTURES_SECONDARY_FEATURE_FLAG)
    }
)

internal val playerGestureConfigSyntheticFingerprint = legacyFingerprint(
    name = "playerGestureConfigSyntheticFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,      // playerGestureConfig.downAndOutLandscapeAllowed
        Opcode.MOVE_RESULT,
        Opcode.CHECK_CAST,
        Opcode.IPUT_BOOLEAN,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,      // playerGestureConfig.downAndOutPortraitAllowed
        Opcode.MOVE_RESULT,
        Opcode.IPUT_BOOLEAN,
        Opcode.RETURN_VOID,
    ),
    customFingerprint = { method, classDef ->
        // This method is always called "a" because this kind of class always has a single method.
        method.name == "a" &&
                classDef.methods.count() == 2 &&
                indexOfPlayerConfigModelBooleanInstruction(method) >= 0
    },
)

internal fun indexOfPlayerConfigModelBooleanInstruction(
    method: Method,
    startIndex: Int = 0
) = method.indexOfFirstInstruction(startIndex) {
    val reference = getReference<MethodReference>()
    opcode == Opcode.INVOKE_VIRTUAL &&
            reference?.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/media/PlayerConfigModel;" &&
            reference.parameterTypes.isEmpty() &&
            reference.returnType == "Z"
}