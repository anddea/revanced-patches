/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.swipe.controls

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.string
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.util.getReference
import app.morphe.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.morphe.patches.youtube.utils.resourceid.autoNavScrollCancelPadding
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object hdrBrightnessFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(string("mediaViewambientBrightnessSensor", StringComparisonType.CONTAINS))
)

internal object swipeControlsHostActivityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.type == "$EXTENSION_PATH/swipecontrols/SwipeControlsHostActivity;"
    }
)

internal const val SWIPE_TO_SWITCH_VIDEO_FEATURE_FLAG = 45631116L

/**
 * This fingerprint is compatible with YouTube v19.19.39+
 */
internal object swipeToSwitchVideoFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    custom = { method, _ -> method.containsLiteralInstruction(SWIPE_TO_SWITCH_VIDEO_FEATURE_FLAG) },
)

internal const val WATCH_PANEL_GESTURES_PRIMARY_FEATURE_FLAG = 45372793L

/**
 * This fingerprint is compatible with YouTube v18.29.38 ~ v19.34.42
 */
internal object watchPanelGesturesFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, _ -> method.containsLiteralInstruction(WATCH_PANEL_GESTURES_PRIMARY_FEATURE_FLAG) },
)

internal object watchPanelGesturesAlternativeFingerprint : Fingerprint(
    custom = { method, _ -> method.containsLiteralInstruction(autoNavScrollCancelPadding) },
)

internal const val WATCH_PANEL_GESTURES_SECONDARY_FEATURE_FLAG = 45619395L

/**
 * Watch panel gestures in channel bar
 * This fingerprint is compatible with YouTube v19.15.36 ~
 */
internal object watchPanelGesturesChannelBarFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/view/MotionEvent;"),
    custom = { method, _ ->
        method.name == "onInterceptTouchEvent" &&
                method.containsLiteralInstruction(WATCH_PANEL_GESTURES_SECONDARY_FEATURE_FLAG)
    }
)

internal object PlayerDragGestureTypeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "Ljava/lang/String;",
    parameters = listOf("I"),
    strings = listOf(
        "FULLSCREEN_DRAGGED_DOWN",
        "MAXIMIZED_PULLED_UP"
    )
)

internal object PlayerDragGestureInitFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "I",
    parameters = listOf("I", "I", "I", "I"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET,
        Opcode.IF_NE,
        Opcode.IGET,
        Opcode.IF_NE,
        Opcode.CONST_4,
        Opcode.RETURN
    ) + fieldAccess(
        opcode = Opcode.IGET_OBJECT,
        type = "/NextGenWatchLayout;",
        location = MatchAfterImmediately()
    )
)

internal object playerGestureConfigSyntheticFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    filters = OpcodesFilter.opcodesToFilters(
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
    custom = { method, classDef ->
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

/**
 * YouTube player pinch-to-zoom listener.
 *
 * Unique among other ScaleGestureDetector listeners (photo picker, creation, etc.):
 * it implements only OnScaleGestureListener and has both ScaleGestureDetector and GestureDetector fields.
 */
internal object VideoZoomScaleBeginFingerprint : Fingerprint(
    name = "onScaleBegin",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/view/ScaleGestureDetector;"),
    custom = { _, classDef ->
        classDef.interfaces.size == 1 &&
                classDef.interfaces.contains($$"Landroid/view/ScaleGestureDetector$OnScaleGestureListener;") &&
                classDef.fields.any { it.type == "Landroid/view/ScaleGestureDetector;" } &&
                classDef.fields.any { it.type == "Landroid/view/GestureDetector;" }
    }
)

/**
 * Touch events of the player, in the class of [VideoZoomScaleBeginFingerprint].
 */
internal object VideoZoomTouchFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/view/View;", "Landroid/view/MotionEvent;")
)
