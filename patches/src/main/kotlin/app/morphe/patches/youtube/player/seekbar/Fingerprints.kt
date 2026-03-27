package app.morphe.patches.youtube.player.seekbar

import app.morphe.patches.youtube.utils.resourceid.inlineTimeBarLiveSeekAbleRange
import app.morphe.patches.youtube.utils.resourceid.reelTimeBarPlayedColor
import app.morphe.patches.youtube.utils.resourceid.ytStaticBrandRed
import app.morphe.patches.youtube.utils.resourceid.ytTextSecondary
import app.morphe.patches.youtube.utils.resourceid.ytYoutubeMagenta
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val shortsSeekbarColorFingerprint = legacyFingerprint(
    name = "shortsSeekbarColorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(reelTimeBarPlayedColor),
)

internal val controlsOverlayStyleFingerprint = legacyFingerprint(
    name = "controlsOverlayStyleFingerprint",
    opcodes = listOf(Opcode.CONST_HIGH16),
    strings = listOf("YOUTUBE", "PREROLL", "POSTROLL"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/ControlsOverlayStyle;")
    }
)

internal const val PLAYER_SEEKBAR_GRADIENT_FEATURE_FLAG = 45617850L

internal val playerSeekbarGradientConfigFingerprint = legacyFingerprint(
    name = "playerSeekbarGradientConfigFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(PLAYER_SEEKBAR_GRADIENT_FEATURE_FLAG),
)

internal val playerSeekbarHandleColorPrimaryFingerprint = legacyFingerprint(
    name = "playerSeekbarHandleColorPrimaryFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("Landroid/content/Context;"),
    literals = listOf(ytTextSecondary, ytStaticBrandRed),
)

internal val playerSeekbarHandleColorSecondaryFingerprint = legacyFingerprint(
    name = "playerSeekbarHandleColorSecondaryFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(inlineTimeBarLiveSeekAbleRange, ytStaticBrandRed),
)

internal val watchHistoryMenuUseProgressDrawableFingerprint = legacyFingerprint(
    name = "watchHistoryMenuUseProgressDrawableFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("L"),
    literals = listOf(-1712394514),
)

internal val lithoLinearGradientFingerprint = legacyFingerprint(
    name = "lithoLinearGradientFingerprint",
    accessFlags = AccessFlags.STATIC.value,
    returnType = "Landroid/graphics/LinearGradient;",
    parameters = listOf("F", "F", "F", "F", "[I", "[F")
)

/**
 * YouTube 19.49+
 */
internal val playerLinearGradientFingerprint = legacyFingerprint(
    name = "playerLinearGradientFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("I", "I", "I", "I", "Landroid/content/Context;", "I"),
    returnType = "Landroid/graphics/LinearGradient;",
    opcodes = listOf(
        Opcode.FILLED_NEW_ARRAY,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literals = listOf(ytYoutubeMagenta),
)

/**
 * YouTube 19.25 - 19.47
 */
internal val playerLinearGradientLegacyFingerprint = legacyFingerprint(
    name = "playerLinearGradientLegacyFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.FILLED_NEW_ARRAY,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literals = listOf(ytYoutubeMagenta),
)

internal const val launchScreenLayoutTypeLotteFeatureLegacyFlag = 268507948L
internal const val launchScreenLayoutTypeLotteFeatureFlag = 1073814316L

internal val setBoundsFingerprint = legacyFingerprint(
    name = "setBoundsFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("I", "I", "I", "I"),
    opcodes = listOf(
        Opcode.NEW_ARRAY,
        Opcode.FILL_ARRAY_DATA
    )
)

internal val seekbarThumbFingerprint = legacyFingerprint(
    name = "seekbarThumbFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL
    )
)

internal val launchScreenLayoutTypeFingerprint = legacyFingerprint(
    name = "launchScreenLayoutTypeFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    customFingerprint = { method, _ ->
        val firstParameter = method.parameterTypes.firstOrNull()
        // 19.25 - 19.45
        (firstParameter == "Lcom/google/android/apps/youtube/app/watchwhile/MainActivity;"
                || firstParameter == "Landroid/app/Activity;") // 19.46+
                && (method.containsLiteralInstruction(launchScreenLayoutTypeLotteFeatureLegacyFlag)
                || method.containsLiteralInstruction(launchScreenLayoutTypeLotteFeatureFlag))
    }
)

internal val seekbarTappingFingerprint = legacyFingerprint(
    name = "seekbarTappingFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/MotionEvent;"),
    customFingerprint = { method, classDef ->
        classDef.interfaces.contains("Landroid/view/View${'$'}OnLayoutChangeListener;") &&
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

internal val seekbarThumbnailsQualityFingerprint = legacyFingerprint(
    name = "seekbarThumbnailsQualityFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(45399684L),
)

internal val thumbnailPreviewConfigFingerprint = legacyFingerprint(
    name = "thumbnailPreviewConfigFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(45398577L),
)

internal val timeCounterFingerprint = legacyFingerprint(
    name = "timeCounterFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    returnType = "V",
    opcodes = listOf(
        Opcode.SUB_LONG_2ADDR,
        Opcode.IGET_WIDE,
        Opcode.SUB_LONG_2ADDR
    )
)

internal val timelineMarkerArrayFingerprint = legacyFingerprint(
    name = "timelineMarkerArrayFingerprint",
    returnType = "[Lcom/google/android/libraries/youtube/player/features/overlay/timebar/TimelineMarker;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
)
