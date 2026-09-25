package app.morphe.patches.youtube.utils

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import app.morphe.patches.youtube.player.components.playerComponentsPatch
import app.morphe.patches.youtube.utils.resourceid.fadeDurationFast
import app.morphe.patches.youtube.utils.resourceid.inlineTimeBarColorizedBarPlayedColorDark
import app.morphe.patches.youtube.utils.resourceid.inlineTimeBarPlayedNotHighlightedColor
import app.morphe.patches.youtube.utils.resourceid.insetOverlayViewLayout
import app.morphe.patches.youtube.utils.resourceid.menuItemView
import app.morphe.patches.youtube.utils.resourceid.playerControlNextButtonTouchArea
import app.morphe.patches.youtube.utils.resourceid.playerControlPreviousButtonTouchArea
import app.morphe.patches.youtube.utils.resourceid.scrimOverlay
import app.morphe.patches.youtube.utils.resourceid.seekUndoEduOverlayStub
import app.morphe.patches.youtube.utils.resourceid.settingsFragment
import app.morphe.patches.youtube.utils.resourceid.settingsFragmentCairo
import app.morphe.patches.youtube.utils.resourceid.totalTime
import app.morphe.patches.youtube.utils.resourceid.varispeedUnavailableTitle
import app.morphe.patches.youtube.utils.resourceid.videoQualityBottomSheet
import app.morphe.patches.youtube.utils.resourceid.youTubeControlsButtonGroupLayoutStub
import app.morphe.patches.youtube.utils.sponsorblock.sponsorBlockBytecodePatch
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.containsStringInstruction
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE =
    "Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;"

internal const val YOUTUBE_PIVOT_BAR_CLASS_TYPE =
    "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"

internal const val YOUTUBE_VIDEO_QUALITY_CLASS_TYPE =
    "Lcom/google/android/libraries/youtube/innertube/model/media/VideoQuality;"

internal val bottomSheetMenuItemBuilderFingerprint = "bottomSheetMenuItemBuilderFingerprint" to Fingerprint(
    returnType = "L",
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET,
        Opcode.AND_INT_LIT16,
        Opcode.IF_EQZ,
    ),
    strings = listOf("Text missing for BottomSheetMenuItem."),
    custom = { method, _ ->
        indexOfSpannedCharSequenceInstruction(method) >= 0
    }
)

fun indexOfSpannedCharSequenceInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_STATIC &&
                reference?.parameterTypes?.size == 1 &&
                reference.returnType == "Ljava/lang/CharSequence;"
    }

/**
 * Added in YouTube v19.04.38
 *
 * When this value is TRUE, Cairo Fragment is used.
 * In this case, some of patches may be broken, so set this value to FALSE.
 */
internal const val CAIRO_FRAGMENT_FEATURE_FLAG = 45532100L

internal val cairoFragmentConfigFingerprint = "cairoFragmentConfigFingerprint" to Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { method, _ ->
        method.containsLiteralInstruction(CAIRO_FRAGMENT_FEATURE_FLAG)
    },
)

internal val layoutConstructorFingerprint = "layoutConstructorFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { method, _ ->
        method.containsLiteralInstruction(playerControlPreviousButtonTouchArea) &&
                method.containsLiteralInstruction(playerControlNextButtonTouchArea)
    },
)

internal val inflateControlsGroupLayoutStubFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "V",
    filters = listOf(literal(youTubeControlsButtonGroupLayoutStub))
)

internal val playbackRateBottomSheetBuilderFingerprint = "playbackRateBottomSheetBuilderFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
    ),
    custom = { method, _ ->
        method.containsLiteralInstruction(varispeedUnavailableTitle)
    },
)

internal val playerButtonsResourcesFingerprint = "playerButtonsResourcesFingerprint" to Fingerprint(
    returnType = "I",
    parameters = listOf("Landroid/content/res/Resources;"),
    custom = { method, _ ->
        method.containsLiteralInstruction(17694721L)
    },
)

internal val playerButtonsVisibilityFingerprint = "playerButtonsVisibilityFingerprint" to Fingerprint(
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE
    ),
    parameters = listOf("Z", "Z")
)

internal val playerSeekbarColorFingerprint = "playerSeekbarColorFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    custom = { method, _ ->
        method.containsLiteralInstruction(inlineTimeBarColorizedBarPlayedColorDark) &&
                method.containsLiteralInstruction(inlineTimeBarPlayedNotHighlightedColor)
    },
)

internal val qualityMenuViewInflateFingerprint = "qualityMenuViewInflateFingerprint" to Fingerprint(
    returnType = "L",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "L", "L"),
    custom = custom@{ method, _ ->
        if (!method.containsLiteralInstruction(videoQualityBottomSheet)) {
            return@custom false
        }
        if (indexOfAddHeaderViewInstruction(method) < 0) {
            return@custom false
        }
        val implementation = method.implementation
            ?: return@custom false

        implementation.instructions.elementAt(0).opcode == Opcode.INVOKE_SUPER
    }
)

internal fun indexOfAddHeaderViewInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "addHeaderView"
    }

internal val rollingNumberTextViewAnimationUpdateFingerprint = "rollingNumberTextViewAnimationUpdateFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/graphics/Bitmap;"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.NEW_INSTANCE, // bitmap ImageSpan
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    )
)

/**
 * This fingerprint is compatible with YouTube v18.32.39+
 */
internal val rollingNumberTextViewFingerprint = "rollingNumberTextViewFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "F", "F"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IPUT,
        null,   // invoke-direct or invoke-virtual
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    custom = custom@{ _, classDef ->
        classDef.superclass == "Landroid/support/v7/widget/AppCompatTextView;"
                || classDef.superclass == "Lcom/google/android/libraries/youtube/rendering/ui/spec/typography/YouTubeAppCompatTextView;"
    }
)

internal val scrollTopParentFingerprint = "scrollTopParentFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.CONST_16,
        Opcode.INVOKE_VIRTUAL,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.RETURN_VOID
    ),
    custom = { method, _ -> method.name == "<init>" }
)

internal val seekbarFingerprint = "seekbarFingerprint" to Fingerprint(
    returnType = "V",
    strings = listOf("timed_markers_width")
)

internal val seekbarOnDrawFingerprint = "seekbarOnDrawFingerprint" to Fingerprint(
    custom = { method, _ -> method.name == "onDraw" }
)

internal fun indexOfGetDrawableInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.toString() == "Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;"
    }

internal val settingsFragmentSyntheticFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        opcode(Opcode.INVOKE_VIRTUAL_RANGE),
    ),
    custom = { method, _ ->
        method.containsLiteralInstruction(settingsFragment) &&
                method.containsLiteralInstruction(settingsFragmentCairo)
    },
)

internal val toolBarButtonFingerprint = "toolBarButtonFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { method, _ ->
        val parameters = method.parameterTypes

        parameters.firstOrNull() == "Landroid/view/MenuItem;" &&
                (parameters.size == 1 ||
                        (parameters.size == 2 && parameters[1] == "Landroid/content/Context;")) &&
                method.containsLiteralInstruction(menuItemView) &&
                indexOfGetDrawableInstruction(method) >= 0
    }
)

internal val totalTimeFingerprint = "totalTimeFingerprint" to Fingerprint(
    returnType = "V",
    custom = { method, _ ->
        method.containsLiteralInstruction(totalTime)
    },
)

internal val videoEndFingerprint = "videoEndFingerprint" to Fingerprint(
    strings = listOf("Attempting to seek during an ad"),
    custom = { method, _ ->
        method.containsLiteralInstruction(45368273L)
    },
)

/**
 * This fingerprint is compatible with all versions of YouTube starting from v18.29.38 to supported versions.
 * This method is invoked only in Shorts.
 * Accurate video information is invoked even when the user moves Shorts upward or downward.
 */
internal val videoIdFingerprintShorts = "videoIdFingerprintShorts" to Fingerprint(
    returnType = "V",
    // PlayerResponseModel is an obfuscated interface from 21.04 onward.
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT
    ),
    custom = { method, classDef ->
        val isPlayerResponse = method.parameterTypes.first() == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR ||
                classDef.methods.any { it.containsStringInstruction("\$ReelSequenceControllerStateKey") }
        isPlayerResponse && (method.containsLiteralInstruction(45365621L) ||
                method.indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_STATIC &&
                            getReference<MethodReference>()?.toString() == "Ljava/nio/ByteBuffer;->wrap([B)Ljava/nio/ByteBuffer;"
                } >= 0)
    }
)

/**
 * Several instructions are added to this method by different patches.
 * Therefore, patches using this fingerprint should not use the [Opcode] pattern,
 * and must access the index through the resourceId.
 *
 * The patches and resourceIds that use this fingerprint are as follows:
 * - [playerComponentsPatch] uses [fadeDurationFast], [scrimOverlay] and [seekUndoEduOverlayStub].
 * - [sponsorBlockBytecodePatch] uses [insetOverlayViewLayout].
 */
internal val youtubeControlsOverlayFingerprint = "youtubeControlsOverlayFingerprint" to Fingerprint(
    custom = { method, _ ->
        listOf(
            // Removed in YouTube 20.09.40+
            // eduOverlayStub,
            // fadeDurationFast,
            insetOverlayViewLayout,
            scrimOverlay,
            // Removed in YouTube 20.02.38+
            // seekUndoEduOverlayStub
        ).all { method.containsLiteralInstruction(it) } &&
                indexOfFocusableInTouchModeInstruction(method) >= 0
    }
)

internal fun indexOfFocusableInTouchModeInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setFocusableInTouchMode"
    }

const val PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR =
    "Lcom/google/android/libraries/youtube/innertube/model/player/PlayerResponseModel;"
