@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.player.components

import app.revanced.patches.youtube.utils.resourceid.componentLongClickListener
import app.revanced.patches.youtube.utils.resourceid.darkBackground
import app.revanced.patches.youtube.utils.resourceid.donationCompanion
import app.revanced.patches.youtube.utils.resourceid.easySeekEduContainer
import app.revanced.patches.youtube.utils.resourceid.endScreenElementLayoutCircle
import app.revanced.patches.youtube.utils.resourceid.endScreenElementLayoutIcon
import app.revanced.patches.youtube.utils.resourceid.endScreenElementLayoutVideo
import app.revanced.patches.youtube.utils.resourceid.offlineActionsVideoDeletedUndoSnackbarText
import app.revanced.patches.youtube.utils.resourceid.scrubbing
import app.revanced.patches.youtube.utils.resourceid.seekEasyHorizontalTouchOffsetToStartScrubbing
import app.revanced.patches.youtube.utils.resourceid.suggestedAction
import app.revanced.patches.youtube.utils.resourceid.tapBloomView
import app.revanced.patches.youtube.utils.resourceid.touchArea
import app.revanced.patches.youtube.utils.resourceid.videoZoomSnapIndicator
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
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

/**
 * This value restores the 'Slide to seek' behavior.
 * Deprecated in YouTube v19.18.41+.
 */
internal val restoreSlideToSeekBehaviorFingerprint = legacyFingerprint(
    name = "restoreSlideToSeekBehaviorFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    opcodes = listOf(Opcode.MOVE_RESULT),
    literals = listOf(45411329L),
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

/**
 * This value disables 'Playing at 2x speed' while holding down.
 * Deprecated in YouTube v19.18.41+.
 */
internal val speedOverlayFingerprint = legacyFingerprint(
    name = "speedOverlayFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    opcodes = listOf(Opcode.MOVE_RESULT),
    literals = listOf(45411330L),
)

/**
 * This value is the key for the playback speed overlay value.
 * Deprecated in YouTube v19.18.41+.
 */
internal val speedOverlayFloatValueFingerprint = legacyFingerprint(
    name = "speedOverlayFloatValueFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(Opcode.DOUBLE_TO_FLOAT),
    literals = listOf(45411328L),
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

internal val filmStripOverlayParentFingerprint = legacyFingerprint(
    name = "filmStripOverlayParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(scrubbing),
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

internal val infoCardsIncognitoFingerprint = legacyFingerprint(
    name = "infoCardsIncognitoFingerprint",
    returnType = "Ljava/lang/Boolean;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "J"),
    opcodes = listOf(Opcode.IGET_BOOLEAN),
    strings = listOf("vibrator")
)

internal val layoutCircleFingerprint = legacyFingerprint(
    name = "layoutCircleFingerprint",
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

internal val layoutIconFingerprint = legacyFingerprint(
    name = "layoutIconFingerprint",
    returnType = "Landroid/view/View;",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    ),
    literals = listOf(endScreenElementLayoutIcon),
)

internal val layoutVideoFingerprint = legacyFingerprint(
    name = "layoutVideoFingerprint",
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

internal val seekEduContainerFingerprint = legacyFingerprint(
    name = "seekEduContainerFingerprint",
    returnType = "V",
    literals = listOf(easySeekEduContainer),
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
