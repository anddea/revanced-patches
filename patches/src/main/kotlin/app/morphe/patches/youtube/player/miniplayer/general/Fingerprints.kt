/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

@file:Suppress("SpellCheckingInspection")

package app.morphe.patches.youtube.player.miniplayer.general

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.youtube.utils.resourceid.floatyBarTopMargin
import app.morphe.patches.youtube.utils.resourceid.miniplayerMaxSize
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerClose
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerExpand
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerForwardButton
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerOverlayActionButton
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerRewindButton
import app.morphe.patches.youtube.utils.resourceid.scrimOverlay
import app.morphe.patches.youtube.utils.resourceid.ytOutlinePictureInPictureWhite
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// region legacy miniplayer

internal val miniplayerDimensionsCalculatorParentFingerprint = legacyFingerprint(
    name = "miniplayerDimensionsCalculatorParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(floatyBarTopMargin),
)

/**
 * Matches using the class found in [miniplayerDimensionsCalculatorParentFingerprint].
 */
internal val miniplayerOverrideNoContextFingerprint = legacyFingerprint(
    name = "miniplayerOverrideNoContextFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "Z",
    opcodes = listOf(Opcode.IGET_BOOLEAN), // anchor to insert the instruction
)

internal val miniplayerOverrideFingerprint = legacyFingerprint(
    name = "miniplayerOverrideFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("appName")
)

internal val miniplayerResponseModelSizeCheckFingerprint = legacyFingerprint(
    name = "miniplayerResponseModelSizeCheckFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.RETURN_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
    )
)

// endregion

// region modern miniplayer

internal const val MINIPLAYER_MODERN_FEATURE_KEY = 45622882L

// In later targets this feature flag does nothing and is dead code.
internal const val MINIPLAYER_MODERN_FEATURE_LEGACY_KEY = 45630429L
internal const val MINIPLAYER_DOUBLE_TAP_FEATURE_KEY = 45628823L
internal const val MINIPLAYER_DRAG_DROP_FEATURE_KEY = 45628752L
internal const val MINIPLAYER_HORIZONTAL_DRAG_FEATURE_KEY = 45658112L
internal const val MINIPLAYER_ROUNDED_CORNERS_FEATURE_KEY = 45652224L
internal const val MINIPLAYER_INITIAL_SIZE_FEATURE_KEY = 45640023L
internal const val MINIPLAYER_DISABLED_FEATURE_KEY = 45657015L
internal const val MINIPLAYER_ANIMATED_EXPAND_FEATURE_KEY = 45644360L

// If the value of this flag is FALSE, dismissing the miniplayer via swipe down is allowed.
// Currently, only YouTube 19.16.39 requires overriding this flag.
internal const val MINIPLAYER_SWIPE_TO_DISMISS_FEATURE_KEY = 45622882L

internal val miniplayerModernConstructorFingerprint = legacyFingerprint(
    name = "miniplayerModernConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(45623000L),
)

/**
 * Matches the animation callback used when horizontal drag moves the miniplayer off-screen.
 */
internal object MiniplayerHorizontalDragPlaybackFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = $$"Landroid/animation/ValueAnimator;->addUpdateListener(Landroid/animation/ValueAnimator$AnimatorUpdateListener;)V",
        ),
        opcode(
            opcode = Opcode.NEW_INSTANCE,
            location = MatchAfterWithin(4)
        ),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            name = "<init>",
            location = MatchAfterWithin(4)
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = $$"Landroid/animation/ValueAnimator;->addListener(Landroid/animation/Animator$AnimatorListener;)V",
            location = MatchAfterWithin(4)
        ),
        opcode(
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterWithin(4)
        ),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            returnType = "Ljava/lang/Object;",
            location = MatchAfterWithin(4)
        )
    )
)

/**
 * Matches the Rect drag method so its previous bounds and screen width fields can be reused.
 */
internal object MiniplayerRectDragFieldsNameFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/graphics/Rect;",
    parameters = listOf("I", "I"),
    filters = listOf(
        opcode(opcode = Opcode.IF_GEZ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Landroid/graphics/Rect;",
            location = MatchAfterImmediately()
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/graphics/Rect;->width()I",
            location = MatchAfterImmediately()
        ),
        opcode(
            opcode = Opcode.MOVE_RESULT,
            location = MatchAfterImmediately()
        ),
        opcode(
            opcode = Opcode.NEG_INT,
            location = MatchAfterImmediately()
        ),
        opcode(
            opcode = Opcode.GOTO,
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET,
            type = "I",
            location = MatchAfterImmediately()
        )
    )
)

internal object MiniplayerHorizontalRepositionFingerprint : Fingerprint(
    classFingerprint = MiniplayerRectDragFieldsNameFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/graphics/Rect;"),
)

internal object NextGenWatchLayoutOnInterceptTouchEventFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/watch/nextgenwatch/ui/NextGenWatchLayout;",
    name = "onInterceptTouchEvent",
    parameters = listOf("Landroid/view/MotionEvent;")
)

internal val miniplayerModernViewParentFingerprint = legacyFingerprint(
    name = "miniplayerModernViewParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("player_overlay_modern_mini_player_controls")
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernAddViewListenerFingerprint = legacyFingerprint(
    name = "miniplayerModernAddViewListenerFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Landroid/view/View;")
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernCloseButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernCloseButtonFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerClose),
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernExpandButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernExpandButtonFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerExpand),
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernExpandCloseDrawablesFingerprint = legacyFingerprint(
    name = "miniplayerModernExpandCloseDrawablesFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(ytOutlinePictureInPictureWhite),
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernForwardButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernForwardButtonFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerForwardButton),
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernOverlayViewFingerprint = legacyFingerprint(
    name = "miniplayerModernOverlayViewFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(scrimOverlay),
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernRewindButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernRewindButtonFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerRewindButton),
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernActionButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernActionButtonFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerOverlayActionButton),
)

internal val miniplayerMinimumSizeFingerprint = legacyFingerprint(
    name = "miniplayerMinimumSizeFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(192L, 128L, miniplayerMaxSize),
)

internal val miniplayerOnCloseHandlerFingerprint = legacyFingerprint(
    name = "miniplayerOnCloseHandlerFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    literals = listOf(MINIPLAYER_DISABLED_FEATURE_KEY),
)

internal val miniplayerModernSwipeToDismissFingerprint = legacyFingerprint(
    name = "miniplayerModernSwipeToDismissFingerprint",
    literals = listOf(MINIPLAYER_SWIPE_TO_DISMISS_FEATURE_KEY),
)

internal const val YOUTUBE_PLAYER_OVERLAYS_LAYOUT_CLASS_NAME =
    "Lcom/google/android/apps/youtube/app/common/player/overlay/YouTubePlayerOverlaysLayout;"

internal val youTubePlayerOverlaysLayoutFingerprint = legacyFingerprint(
    name = "youTubePlayerOverlaysLayoutFingerprint",
    customFingerprint = { _, classDef ->
        classDef.type == YOUTUBE_PLAYER_OVERLAYS_LAYOUT_CLASS_NAME
    }
)
