@file:Suppress("SpellCheckingInspection")

package app.morphe.patches.youtube.player.components

import app.morphe.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.resourceid.componentLongClickListener
import app.morphe.patches.youtube.utils.resourceid.darkBackground
import app.morphe.patches.youtube.utils.resourceid.donationCompanion
import app.morphe.patches.youtube.utils.resourceid.easySeekEduContainer
import app.morphe.patches.youtube.utils.resourceid.endScreenElementLayoutCircle
import app.morphe.patches.youtube.utils.resourceid.endScreenElementLayoutIcon
import app.morphe.patches.youtube.utils.resourceid.endScreenElementLayoutVideo
import app.morphe.patches.youtube.utils.resourceid.offlineActionsVideoDeletedUndoSnackbarText
import app.morphe.patches.youtube.utils.resourceid.seekEasyHorizontalTouchOffsetToStartScrubbing
import app.morphe.patches.youtube.utils.resourceid.suggestedAction
import app.morphe.patches.youtube.utils.resourceid.tapBloomView
import app.morphe.patches.youtube.utils.resourceid.touchArea
import app.morphe.patches.youtube.utils.resourceid.verticalTouchOffsetToEnterFineScrubbing
import app.morphe.patches.youtube.utils.resourceid.verticalTouchOffsetToStartFineScrubbing
import app.morphe.patches.youtube.utils.resourceid.videoZoomSnapIndicator
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val horizontalTouchOffsetConstructorFingerprint = legacyFingerprint(
    name = "horizontalTouchOffsetConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(seekEasyHorizontalTouchOffsetToStartScrubbing),
)

internal val nextGenWatchLayoutFingerprint = legacyFingerprint(
    name = "nextGenWatchLayoutFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = handler@{ method, _ ->
        if (method.definingClass != "Lcom/google/android/apps/youtube/app/watch/nextgenwatch/ui/NextGenWatchLayout;")
            return@handler false

        method.indexOfFirstInstruction {
            getReference<MethodReference>()?.name == "booleanValue"
        } >= 0
    }
)

internal const val RESTORE_SLIDE_TO_SEEK_FEATURE_FLAG = 45411329L

/**
 * This value restores the 'Slide to seek' behavior.
 * Deprecated in YouTube v19.18.41+.
 */
internal val restoreSlideToSeekBehaviorFingerprint = legacyFingerprint(
    name = "restoreSlideToSeekBehaviorFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    opcodes = listOf(Opcode.MOVE_RESULT),
    literals = listOf(RESTORE_SLIDE_TO_SEEK_FEATURE_FLAG),
)

internal val slideToSeekMotionEventFingerprint = legacyFingerprint(
    name = "slideToSeekMotionEventFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;", "Landroid/view/MotionEvent;"),
    opcodes = listOf(
        Opcode.SUB_FLOAT_2ADDR,
        Opcode.INVOKE_VIRTUAL,  // SlideToSeek Boolean method
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,     // insert index
        Opcode.INVOKE_VIRTUAL
    )
)

internal const val SPEED_OVERLAY_FEATURE_FLAG = 45411330L

/**
 * This value disables 'Playing at 2x speed' while holding down.
 * Deprecated in YouTube v19.18.41+.
 */
internal val speedOverlayFingerprint = legacyFingerprint(
    name = "speedOverlayFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    opcodes = listOf(Opcode.MOVE_RESULT),
    literals = listOf(SPEED_OVERLAY_FEATURE_FLAG),
)

internal const val SPEED_OVERLAY_LEGACY_FEATURE_FLAG = 45411328L

/**
 * This value is the key for the playback speed overlay value.
 * Deprecated in YouTube v19.18.41+.
 */
internal val speedOverlayFloatValueFingerprint = legacyFingerprint(
    name = "speedOverlayFloatValueFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(Opcode.DOUBLE_TO_FLOAT),
    literals = listOf(SPEED_OVERLAY_LEGACY_FEATURE_FLAG),
)

internal val speedOverlayTextValueFingerprint = legacyFingerprint(
    name = "speedOverlayTextValueFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.CONST_WIDE_HIGH16),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            getReference<MethodReference>()?.toString() == "Ljava/math/BigDecimal;->signum()I"
        } >= 0
    }
)

internal val crowdfundingBoxFingerprint = legacyFingerprint(
    name = "crowdfundingBoxFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT
    ),
    literals = listOf(donationCompanion),
)

internal val doubleTapInfoConstructorFingerprint = legacyFingerprint(
    name = "doubleTapInfoConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf(
        "Landroid/view/MotionEvent;",
        "I",
        "Z",
        "Lj\$/time/Duration;"
    )
)

internal val doubleTapInfoFloatFingerprint = legacyFingerprint(
    name = "doubleTapInfoFloatFingerprint",
    returnType = "I",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("I", "I", "Z"),
    literals = listOf(1051372203L, 2.0f.toRawBits().toLong())
)

internal val doubleTapInfoGetSeekSourceFingerprint = legacyFingerprint(
    name = "doubleTapInfoGetSeekSourceFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.SGET_OBJECT,
    )
)

internal val endScreenElementLayoutCircleFingerprint = legacyFingerprint(
    name = "endScreenElementLayoutCircleFingerprint",
    returnType = "Landroid/view/View;",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    ),
    literals = listOf(endScreenElementLayoutCircle),
)

internal val endScreenElementLayoutIconFingerprint = legacyFingerprint(
    name = "endScreenElementLayoutIconFingerprint",
    returnType = "Landroid/view/View;",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    ),
    literals = listOf(endScreenElementLayoutIcon),
)

internal val endScreenElementLayoutVideoFingerprint = legacyFingerprint(
    name = "endScreenElementLayoutVideoFingerprint",
    returnType = "Landroid/view/View;",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    ),
    literals = listOf(endScreenElementLayoutVideo),
)

internal val endScreenPlayerResponseModelFingerprint = legacyFingerprint(
    name = "endScreenPlayerResponseModelFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("L"),
    customFingerprint = { method, classDef ->
        classDef.methods.count() == 5
                && method.containsLiteralInstruction(0)
                && method.containsLiteralInstruction(5)
                && method.containsLiteralInstruction(8)
                && method.indexOfFirstInstruction {
            getReference<FieldReference>()?.type == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
        } >= 0
    }
)

/**
 * ~ YouTube 20.11
 */
internal val filmStripOverlayConfigFingerprint = legacyFingerprint(
    name = "filmStripOverlayConfigFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(45381958L),
)

internal val filmStripOverlayInteractionFingerprint = legacyFingerprint(
    name = "filmStripOverlayInteractionFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L")
)

internal val filmStripOverlayEnterParentFingerprint = legacyFingerprint(
    name = "filmStripOverlayEnterParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(verticalTouchOffsetToEnterFineScrubbing),
)

/**
 * YouTube 20.12 ~
 */
internal val filmStripOverlayMotionEventPrimaryFingerprint = legacyFingerprint(
    name = "filmStripOverlayMotionEventPrimaryFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/MotionEvent;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
    ),
)

/**
 * YouTube 20.12 ~
 */
internal val filmStripOverlayMotionEventSecondaryFingerprint = legacyFingerprint(
    name = "filmStripOverlayMotionEventSecondaryFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/MotionEvent;"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.NEG_FLOAT,
    ),
)

internal val filmStripOverlayStartParentFingerprint = legacyFingerprint(
    name = "filmStripOverlayStartParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(verticalTouchOffsetToStartFineScrubbing),
)

internal val filmStripOverlayPreviewFingerprint = legacyFingerprint(
    name = "filmStripOverlayPreviewFingerprint",
    returnType = "Z",
    parameters = listOf("F"),
    opcodes = listOf(
        Opcode.SUB_FLOAT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    )
)

internal const val FILM_STRIP_OVERLAY_V2_FEATURE_FLAG = 45420198L

internal val filmStripOverlayConfigV2Fingerprint = legacyFingerprint(
    name = "filmStripOverlayConfigV2Fingerprint",
    literals = listOf(FILM_STRIP_OVERLAY_V2_FEATURE_FLAG),
)

internal val infoCardsIncognitoFingerprint = legacyFingerprint(
    name = "infoCardsIncognitoFingerprint",
    returnType = "Ljava/lang/Boolean;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "J"),
    opcodes = listOf(Opcode.IGET_BOOLEAN),
    strings = listOf("vibrator")
)

internal val linearLayoutManagerItemCountsFingerprint = legacyFingerprint(
    name = "linearLayoutManagerItemCountsFingerprint",
    returnType = "I",
    accessFlags = AccessFlags.FINAL.value,
    parameters = listOf("L", "L", "L", "Z"),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IF_LEZ,
        Opcode.INVOKE_VIRTUAL,
    ),
    customFingerprint = { method, _ ->
        method.definingClass == "Landroid/support/v7/widget/LinearLayoutManager;"
    }
)

internal val lithoComponentOnClickListenerFingerprint = legacyFingerprint(
    name = "lithoComponentOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    parameters = listOf("L"),
    literals = listOf(componentLongClickListener),
)

internal val engagementPanelPlaylistSyntheticFingerprint = legacyFingerprint(
    name = "engagementPanelPlaylistSyntheticFingerprint",
    strings = listOf("engagement-panel-playlist"),
    customFingerprint = { _, classDef ->
        classDef.interfaces.contains("Landroid/view/View${'$'}OnClickListener;")
    }
)

internal val offlineActionsOnClickListenerFingerprint = legacyFingerprint(
    name = "offlineActionsOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/String;"),
    literals = listOf(offlineActionsVideoDeletedUndoSnackbarText),
)

internal val quickSeekOverlayFingerprint = legacyFingerprint(
    name = "quickSeekOverlayFingerprint",
    returnType = "V",
    parameters = emptyList(),
    literals = listOf(darkBackground, tapBloomView),
)

/**
 * YouTube 18.39 - 20.02
 */
internal val seekEduContainerFingerprint = legacyFingerprint(
    name = "seekEduContainerFingerprint",
    returnType = "V",
    literals = listOf(easySeekEduContainer),
)

/**
 * YouTube 18.39 - 20.02
 */
internal val playerEduOverlayFeatureFlagFingerprint = legacyFingerprint(
    name = "playerEduOverlayFeatureFlagFingerprint",
    returnType = "Z",
    literals = listOf(45427491L),
)

internal val suggestedActionsFingerprint = legacyFingerprint(
    name = "suggestedActionsFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literals = listOf(suggestedAction),
)

internal val touchAreaOnClickListenerFingerprint = legacyFingerprint(
    name = "touchAreaOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(touchArea),
)

internal val videoZoomSnapIndicatorFingerprint = legacyFingerprint(
    name = "videoZoomSnapIndicatorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(videoZoomSnapIndicator),
)

internal val watermarkFingerprint = legacyFingerprint(
    name = "watermarkFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IGET_BOOLEAN
    )
)

internal val watermarkParentFingerprint = legacyFingerprint(
    name = "watermarkParentFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("player_overlay_in_video_programming")
)
