/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.player.seekbar

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.patches.youtube.utils.resourceid.inlineTimeBarLiveSeekAbleRange
import app.morphe.patches.youtube.utils.resourceid.reelTimeBarPlayedColor
import app.morphe.patches.youtube.utils.resourceid.ytStaticBrandRed
import app.morphe.patches.youtube.utils.resourceid.ytTextSecondary
import app.morphe.patches.youtube.utils.resourceid.ytYoutubeMagenta
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ShortsSeekbarColorFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(literal(reelTimeBarPlayedColor)),
)

internal object ControlsOverlayStyleFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(Opcode.CONST_HIGH16),
    strings = listOf("YOUTUBE", "PREROLL", "POSTROLL", "REMOTE_LIVE", "AD_LARGE_CONTROLS"),
)

internal const val PLAYER_SEEKBAR_GRADIENT_FEATURE_FLAG = 45617850L

internal object PlayerSeekbarGradientConfigFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(literal(PLAYER_SEEKBAR_GRADIENT_FEATURE_FLAG)),
)

internal object PlayerSeekbarHandleColorPrimaryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Landroid/content/Context;"),
    custom = { method, _ ->
        method.containsLiteralInstruction(ytTextSecondary) &&
                method.containsLiteralInstruction(ytStaticBrandRed)
    },
)

internal object PlayerSeekbarHandleColorSecondaryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    custom = { method, _ ->
        method.containsLiteralInstruction(inlineTimeBarLiveSeekAbleRange) &&
                method.containsLiteralInstruction(ytStaticBrandRed)
    },
)

internal object WatchHistoryMenuUseProgressDrawableFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(literal(-1712394514)),
)

internal object LithoLinearGradientFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC),
    returnType = "Landroid/graphics/LinearGradient;",
    parameters = listOf("F", "F", "F", "F", "[I", "[F"),
)

/**
 * YouTube 19.49+
 */
internal object PlayerLinearGradientFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("I", "I", "I", "I", "Landroid/content/Context;", "I"),
    returnType = "Landroid/graphics/LinearGradient;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.FILLED_NEW_ARRAY,
        Opcode.MOVE_RESULT_OBJECT
    ),
    custom = { method, _ -> method.containsLiteralInstruction(ytYoutubeMagenta) },
)

/**
 * YouTube 19.25 - 19.47
 */
internal object PlayerLinearGradientLegacyFingerprint : Fingerprint(
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.FILLED_NEW_ARRAY,
        Opcode.MOVE_RESULT_OBJECT
    ),
    custom = { method, _ -> method.containsLiteralInstruction(ytYoutubeMagenta) },
)

internal const val launchScreenLayoutTypeLotteFeatureLegacyFlag = 268507948L
internal const val launchScreenLayoutTypeLotteFeatureFlag = 1073814316L

internal object SetBoundsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "I", "I", "I"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.NEW_ARRAY,
        Opcode.FILL_ARRAY_DATA
    )
)

internal object SeekbarThumbFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL
    )
)

internal object LaunchScreenLayoutTypeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    custom = { method, _ ->
        val firstParameter = method.parameterTypes.firstOrNull()
        // 19.25 - 19.45
        (firstParameter == "Lcom/google/android/apps/youtube/app/watchwhile/MainActivity;"
                || firstParameter == "Landroid/app/Activity;") // 19.46+
                && (method.containsLiteralInstruction(launchScreenLayoutTypeLotteFeatureLegacyFlag)
                || method.containsLiteralInstruction(launchScreenLayoutTypeLotteFeatureFlag))
    }
)

internal object SeekbarTappingFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/view/MotionEvent;"),
    custom = { method, classDef ->
        classDef.interfaces.contains($$"Landroid/view/View$OnLayoutChangeListener;") &&
                classDef.fields.find { it.type == "[Lcom/google/android/libraries/youtube/player/features/overlay/timebar/TimelineMarker;" } != null &&
                method.name == "onTouchEvent" &&
                indexOfPointInstruction(method) >= 0
    }
)

internal fun indexOfPointInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_DIRECT &&
                getReference<MethodReference>()?.toString() == "Landroid/graphics/Point;-><init>(II)V"
    }


internal object TimeCounterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SUB_LONG_2ADDR,
        Opcode.IGET_WIDE,
        Opcode.SUB_LONG_2ADDR
    )
)

internal object TimelineMarkerArrayFingerprint : Fingerprint(
    returnType = "[Lcom/google/android/libraries/youtube/player/features/overlay/timebar/TimelineMarker;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
)

// region Livestream DVR

internal object VideoStreamingDataToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("VideoStreamingData(itags=")
    )
)

internal object VideoStreamingDataAllowSeekingFingerprint : Fingerprint(
    classFingerprint = VideoStreamingDataToStringFingerprint,
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(8),
        opcode(Opcode.IF_EQ, location = MatchAfterImmediately()),
        literal(1, location = MatchAfterImmediately()),
    )
)

private object FormatStreamModelClassFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("FormatStream(itag=")
    )
)

internal object FormatStreamModelMaxDVRDurationFingerprint : Fingerprint(
    classFingerprint = FormatStreamModelClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "D",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        fieldAccess(opcode = Opcode.IGET_WIDE, type = "D", location = MatchAfterImmediately()),
        opcode(Opcode.RETURN_WIDE, location = MatchAfterImmediately()),
    )
)

// endregion

internal object SeekbarFingerprint : Fingerprint (
    returnType = "V",
    filters = listOf(
        string("timed_markers_width")
    )
)

internal object SeekbarHandlerOnTouchFingerprint : Fingerprint (
    classFingerprint = SeekbarFingerprint,
    name = "onTouchEvent"
)

internal object SeekbarUpdatePointFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        fieldAccess(
            definingClass = "this",
            type = "Landroid/graphics/Point;"
        ),
        methodCall( // Get seekbar point.
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf("Landroid/graphics/Point;"),
            returnType = "V",
            location = MatchAfterWithin(5)
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Landroid/graphics/Rect;",
            location = MatchAfterWithin(10)
        ),
        fieldAccess(
            opcode = Opcode.IGET,
            smali = "Landroid/graphics/Rect;->left:I",
            location = MatchAfterWithin(5)
        )
    )
)

internal object SlideSeekbarHandlerOnTouchFingerprint : Fingerprint (
    classFingerprint = Fingerprint (
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
        filters = listOf(
            resourceLiteral(ResourceType.DIMEN, "seek_easy_horizontal_touch_offset_to_start_scrubbing")
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/view/View;", "Landroid/view/MotionEvent;")
)

internal object SlideSeekbarGetViewControllerFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "F"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            location = MatchAfterWithin(10) // Match close to start of method.
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterWithin(10)
        ),
        literal(124587, location = MatchAfterWithin(20)),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterWithin(10)
        ),
        literal(67108864)
    )
)

internal object SeekbarFineScrubbingBitmapFingerprint : Fingerprint (
    classFingerprint = Fingerprint (
        returnType = "Landroid/graphics/Bitmap;",
        parameters = listOf("L", "I", "Landroid/graphics/Bitmap;"),
        filters = listOf(
            string("Storyboard regionDecoder.decodeRegion exception - ")
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED),
    returnType = "V",
    parameters = listOf("Landroid/graphics/Bitmap;")
)

internal object SeekbarBigBoardsUpdateFingerprint : Fingerprint (
    classFingerprint = Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "Ljava/lang/String;",
        parameters = listOf(),
        filters = listOf(
            string("player_overlay_big_boards")
        )
    ),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(1),
        opcode(opcode = Opcode.IF_NEZ, location = MatchAfterImmediately()),
        opcode(opcode = Opcode.RETURN, location = MatchAfterImmediately())
    )
)

internal object SeekbarBigBoardsUpdateLegacyFingerprint : Fingerprint (
    classFingerprint = Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf("Z"),
        filters = listOf(
            fieldAccess(
                opcode = Opcode.SGET_OBJECT,
                smali = $$"Landroid/widget/ImageView$ScaleType;->CENTER_CROP:Landroid/widget/ImageView$ScaleType;"
            ),
            fieldAccess(
                opcode = Opcode.SGET_OBJECT,
                smali = $$"Landroid/widget/ImageView$ScaleType;->FIT_CENTER:Landroid/widget/ImageView$ScaleType;"
            )
        )
    ),
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(1),
        opcode(opcode = Opcode.IF_NEZ, location = MatchAfterImmediately()),
        opcode(opcode = Opcode.RETURN, location = MatchAfterImmediately())
    )
)

internal object ShortsDisableSeekbarThumbnailsFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45787901)
    )
)
