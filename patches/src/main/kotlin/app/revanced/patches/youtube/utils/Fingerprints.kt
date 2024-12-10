package app.revanced.patches.youtube.utils

import app.revanced.patches.youtube.player.components.playerComponentsPatch
import app.revanced.patches.youtube.utils.resourceid.fadeDurationFast
import app.revanced.patches.youtube.utils.resourceid.inlineTimeBarColorizedBarPlayedColorDark
import app.revanced.patches.youtube.utils.resourceid.inlineTimeBarPlayedNotHighlightedColor
import app.revanced.patches.youtube.utils.resourceid.insetOverlayViewLayout
import app.revanced.patches.youtube.utils.resourceid.scrimOverlay
import app.revanced.patches.youtube.utils.resourceid.seekUndoEduOverlayStub
import app.revanced.patches.youtube.utils.resourceid.totalTime
import app.revanced.patches.youtube.utils.resourceid.varispeedUnavailableTitle
import app.revanced.patches.youtube.utils.resourceid.videoQualityBottomSheet
import app.revanced.patches.youtube.utils.sponsorblock.sponsorBlockBytecodePatch
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val engagementPanelBuilderFingerprint = legacyFingerprint(
    name = "engagementPanelBuilderFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L", "L", "Z", "Z"),
    strings = listOf(
        "EngagementPanelController: cannot show EngagementPanel before EngagementPanelController.init() has been called.",
        "[EngagementPanel] Cannot show EngagementPanel before EngagementPanelController.init() has been called."
    )
)

internal val layoutConstructorFingerprint = legacyFingerprint(
    name = "layoutConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("1.0x")
)

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
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
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
    opcodes = listOf(
        Opcode.INVOKE_SUPER,
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_16,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST
    ),
    literals = listOf(videoQualityBottomSheet),
)

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
        fadeDurationFast,
        insetOverlayViewLayout,
        scrimOverlay,
        seekUndoEduOverlayStub
    ),
)

const val PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR =
    "Lcom/google/android/libraries/youtube/innertube/model/player/PlayerResponseModel;"