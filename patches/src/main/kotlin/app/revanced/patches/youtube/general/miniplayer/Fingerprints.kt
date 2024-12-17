@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.general.miniplayer

import app.revanced.patches.youtube.utils.resourceid.floatyBarTopMargin
import app.revanced.patches.youtube.utils.resourceid.miniplayerMaxSize
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerClose
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerExpand
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerForwardButton
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerRewindButton
import app.revanced.patches.youtube.utils.resourceid.scrimOverlay
import app.revanced.patches.youtube.utils.resourceid.ytOutlinePictureInPictureWhite
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val miniplayerDimensionsCalculatorParentFingerprint = legacyFingerprint(
    name = "miniplayerDimensionsCalculatorParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(floatyBarTopMargin),
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
    returnType = "Landroid/widget/ImageView;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerClose),
)

internal const val MINIPLAYER_MODERN_FEATURE_KEY = 45622882L

// In later targets this feature flag does nothing and is dead code.
internal const val MINIPLAYER_MODERN_FEATURE_LEGACY_KEY = 45630429L
internal const val MINIPLAYER_DOUBLE_TAP_FEATURE_KEY = 45628823L
internal const val MINIPLAYER_DRAG_DROP_FEATURE_KEY = 45628752L
internal const val MINIPLAYER_HORIZONTAL_DRAG_FEATURE_KEY = 45658112L
internal const val MINIPLAYER_ROUNDED_CORNERS_FEATURE_KEY = 45652224L
internal const val MINIPLAYER_INITIAL_SIZE_FEATURE_KEY = 45640023L
internal const val MINIPLAYER_DISABLED_FEATURE_KEY = 45657015L

internal val miniplayerModernConstructorFingerprint = legacyFingerprint(
    name = "miniplayerModernConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("L"),
    literals = listOf(45623000L),
)

internal val miniplayerOnCloseHandlerFingerprint = legacyFingerprint(
    name = "miniplayerOnCloseHandlerFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    literals = listOf(MINIPLAYER_DISABLED_FEATURE_KEY),
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernExpandButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernExpandButtonFingerprint",
    returnType = "Landroid/widget/ImageView;",
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
    returnType = "Landroid/widget/ImageView;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerForwardButton),
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernOverlayViewFingerprint = legacyFingerprint(
    name = "miniplayerModernOverlayViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(scrimOverlay),
)

/**
 * Matches using the class found in [miniplayerModernViewParentFingerprint].
 */
internal val miniplayerModernRewindButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernRewindButtonFingerprint",
    returnType = "Landroid/widget/ImageView;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerRewindButton),
)

internal val miniplayerModernViewParentFingerprint = legacyFingerprint(
    name = "miniplayerModernViewParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("player_overlay_modern_mini_player_controls")
)

internal val miniplayerMinimumSizeFingerprint = legacyFingerprint(
    name = "miniplayerMinimumSizeFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(192L, 128L, miniplayerMaxSize),
)

internal val miniplayerOverrideFingerprint = legacyFingerprint(
    name = "miniplayerOverrideFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("appName")
)

internal val miniplayerOverrideNoContextFingerprint = legacyFingerprint(
    name = "miniplayerOverrideNoContextFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "Z",
    opcodes = listOf(Opcode.IGET_BOOLEAN), // anchor to insert the instruction
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

internal const val YOUTUBE_PLAYER_OVERLAYS_LAYOUT_CLASS_NAME =
    "Lcom/google/android/apps/youtube/app/common/player/overlay/YouTubePlayerOverlaysLayout;"

internal val youTubePlayerOverlaysLayoutFingerprint = legacyFingerprint(
    name = "youTubePlayerOverlaysLayoutFingerprint",
    customFingerprint = { _, classDef ->
        classDef.type == YOUTUBE_PLAYER_OVERLAYS_LAYOUT_CLASS_NAME
    }
)
