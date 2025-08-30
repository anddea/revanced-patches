package app.revanced.patches.youtube.utils

import app.revanced.patcher.fingerprint
import app.revanced.patches.youtube.player.components.playerComponentsPatch
import app.revanced.patches.youtube.utils.fix.streamingdata.STREAMING_DATA_OUTER_CLASS
import app.revanced.patches.youtube.utils.resourceid.fadeDurationFast
import app.revanced.patches.youtube.utils.resourceid.fullScreenEngagementPanel
import app.revanced.patches.youtube.utils.resourceid.inlineTimeBarColorizedBarPlayedColorDark
import app.revanced.patches.youtube.utils.resourceid.inlineTimeBarPlayedNotHighlightedColor
import app.revanced.patches.youtube.utils.resourceid.insetOverlayViewLayout
import app.revanced.patches.youtube.utils.resourceid.menuItemView
import app.revanced.patches.youtube.utils.resourceid.playerControlNextButtonTouchArea
import app.revanced.patches.youtube.utils.resourceid.playerControlPreviousButtonTouchArea
import app.revanced.patches.youtube.utils.resourceid.scrimOverlay
import app.revanced.patches.youtube.utils.resourceid.seekUndoEduOverlayStub
import app.revanced.patches.youtube.utils.resourceid.settingsFragment
import app.revanced.patches.youtube.utils.resourceid.settingsFragmentCairo
import app.revanced.patches.youtube.utils.resourceid.totalTime
import app.revanced.patches.youtube.utils.resourceid.varispeedUnavailableTitle
import app.revanced.patches.youtube.utils.resourceid.videoQualityBottomSheet
import app.revanced.patches.youtube.utils.resourceid.youTubeControlsButtonGroupLayoutStub
import app.revanced.patches.youtube.utils.sponsorblock.sponsorBlockBytecodePatch
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.literal
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE =
    "Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;"

internal const val YOUTUBE_PIVOT_BAR_CLASS_TYPE =
    "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"

internal const val YOUTUBE_VIDEO_QUALITY_CLASS_TYPE =
    "Lcom/google/android/libraries/youtube/innertube/model/media/VideoQuality;"

internal val bottomSheetMenuItemBuilderFingerprint = legacyFingerprint(
    name = "bottomSheetMenuItemBuilderFingerprint",
    returnType = "L",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.AND_INT_LIT16,
        Opcode.IF_EQZ,
    ),
    strings = listOf("Text missing for BottomSheetMenuItem."),
    customFingerprint = { method, _ ->
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

internal val cairoFragmentConfigFingerprint = legacyFingerprint(
    name = "cairoFragmentConfigFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(CAIRO_FRAGMENT_FEATURE_FLAG),
)

internal val formatStreamModelToStringFingerprint = legacyFingerprint(
    name = "formatStreamModelToStringFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Ljava/lang/String;",
    customFingerprint = { method, classDef ->
        method.name == "toString"
                && classDef.type == YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE
    }
)

internal val fullScreenEngagementPanelFingerprint = legacyFingerprint(
    name = "fullScreenEngagementPanelFingerprint",
    returnType = "L",
    parameters = listOf("L"),
    literals = listOf(fullScreenEngagementPanel),
)

internal val getEmptyRegistryFingerprint = legacyFingerprint(
    name = "getEmptyRegistryFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = emptyList(),
    returnType = "Lcom/google/protobuf/ExtensionRegistryLite;",
    customFingerprint = { method, classDef ->
        classDef.type == "Lcom/google/protobuf/ExtensionRegistryLite;"
                && method.name != "getGeneratedRegistry"
    },
)

internal val layoutConstructorFingerprint = legacyFingerprint(
    name = "layoutConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(playerControlPreviousButtonTouchArea, playerControlNextButtonTouchArea),
)

internal val inflateControlsGroupLayoutStubFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    returns("V")
    literal { youTubeControlsButtonGroupLayoutStub }
}

internal val playbackRateBottomSheetBuilderFingerprint = legacyFingerprint(
    name = "playbackRateBottomSheetBuilderFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
    ),
    literals = listOf(varispeedUnavailableTitle),
)

internal val playerButtonsResourcesFingerprint = legacyFingerprint(
    name = "playerButtonsResourcesFingerprint",
    returnType = "I",
    parameters = listOf("Landroid/content/res/Resources;"),
    literals = listOf(17694721L),
)

internal val playerButtonsVisibilityFingerprint = legacyFingerprint(
    name = "playerButtonsVisibilityFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE
    ),
    parameters = listOf("Z", "Z")
)

internal val playerSeekbarColorFingerprint = legacyFingerprint(
    name = "playerSeekbarColorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(
        inlineTimeBarColorizedBarPlayedColorDark,
        inlineTimeBarPlayedNotHighlightedColor
    ),
)

internal val qualityMenuViewInflateFingerprint = legacyFingerprint(
    name = "qualityMenuViewInflateFingerprint",
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

        implementation.instructions.elementAt(0).opcode == Opcode.INVOKE_SUPER
    }
)

internal fun indexOfAddHeaderViewInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "addHeaderView"
    }

internal val rollingNumberTextViewAnimationUpdateFingerprint = legacyFingerprint(
    name = "rollingNumberTextViewAnimationUpdateFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/graphics/Bitmap;"),
    opcodes = listOf(
        Opcode.NEW_INSTANCE, // bitmap ImageSpan
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    )
)

/**
 * This fingerprint is compatible with YouTube v18.32.39+
 */
internal val rollingNumberTextViewFingerprint = legacyFingerprint(
    name = "rollingNumberTextViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "F", "F"),
    opcodes = listOf(
        Opcode.IPUT,
        null,   // invoke-direct or invoke-virtual
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = custom@{ _, classDef ->
        classDef.superclass == "Landroid/support/v7/widget/AppCompatTextView;"
                || classDef.superclass == "Lcom/google/android/libraries/youtube/rendering/ui/spec/typography/YouTubeAppCompatTextView;"
    }
)

internal val scrollTopParentFingerprint = legacyFingerprint(
    name = "scrollTopParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
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
    customFingerprint = { method, _ -> method.name == "<init>" }
)

internal val seekbarFingerprint = legacyFingerprint(
    name = "seekbarFingerprint",
    returnType = "V",
    strings = listOf("timed_markers_width")
)

internal val seekbarOnDrawFingerprint = legacyFingerprint(
    name = "seekbarOnDrawFingerprint",
    customFingerprint = { method, _ -> method.name == "onDraw" }
)

internal fun indexOfGetDrawableInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.toString() == "Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;"
    }

internal val settingsFragmentSyntheticFingerprint = legacyFingerprint(
    name = "settingsFragmentSyntheticFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(Opcode.INVOKE_VIRTUAL_RANGE),
    literals = listOf(settingsFragment, settingsFragmentCairo),
)

internal val toolBarButtonFingerprint = legacyFingerprint(
    name = "toolBarButtonFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/MenuItem;"),
    literals = listOf(menuItemView),
    customFingerprint = { method, _ ->
        indexOfGetDrawableInstruction(method) >= 0
    }
)

internal val totalTimeFingerprint = legacyFingerprint(
    name = "totalTimeFingerprint",
    returnType = "V",
    literals = listOf(totalTime),
)

internal val videoEndFingerprint = legacyFingerprint(
    name = "videoEndFingerprint",
    strings = listOf("Attempting to seek during an ad"),
    literals = listOf(45368273L),
)

/**
 * This fingerprint is compatible with all versions of YouTube starting from v18.29.38 to supported versions.
 * This method is invoked only in Shorts.
 * Accurate video information is invoked even when the user moves Shorts upward or downward.
 */
internal val videoIdFingerprintShorts = legacyFingerprint(
    name = "videoIdFingerprintShorts",
    returnType = "V",
    parameters = listOf(PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = custom@{ method, _ ->
        if (method.containsLiteralInstruction(45365621L))
            return@custom true

        method.indexOfFirstInstruction {
            getReference<FieldReference>()?.name == "reelWatchEndpoint"
        } >= 0
    }
)

internal val videoStreamingDataConstructorFingerprint = legacyFingerprint(
    name = "videoStreamingDataConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    customFingerprint = { method, _ ->
        indexOfGetAdaptiveFormatsFieldInstruction(method) >= 0
    },
)

internal fun indexOfGetAdaptiveFormatsFieldInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<FieldReference>()
        opcode == Opcode.IGET_OBJECT &&
                reference?.definingClass == STREAMING_DATA_OUTER_CLASS &&
                // Field f: 'adaptiveFormats'.
                // Field name is always 'f', regardless of the client version.
                reference.name == "f" &&
                reference.type.startsWith("L")
    }

/**
 * On YouTube, this class is 'Lcom/google/android/libraries/youtube/innertube/model/media/VideoStreamingData;'
 * On YouTube Music, class names are obfuscated.
 */
internal val videoStreamingDataToStringFingerprint = legacyFingerprint(
    name = "videoStreamingDataToStringFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("VideoStreamingData(itags="),
    customFingerprint = { method, _ ->
        method.name == "toString"
    },
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
internal val youtubeControlsOverlayFingerprint = legacyFingerprint(
    name = "youtubeControlsOverlayFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(
        // Removed in YouTube 20.09.40+
        // fadeDurationFast,
        insetOverlayViewLayout,
        scrimOverlay,
        // Removed in YouTube 20.02.38+
        // seekUndoEduOverlayStub
    ),
    customFingerprint = { method, _ ->
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
