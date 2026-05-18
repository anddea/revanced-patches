package app.morphe.patches.youtube.player.buttons

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.youtube.utils.resourceid.cfFullscreenButton
import app.morphe.patches.youtube.utils.resourceid.fadeDurationFast
import app.morphe.patches.youtube.utils.resourceid.fullScreenButton
import app.morphe.patches.youtube.utils.resourceid.musicAppDeeplinkButtonView
import app.morphe.patches.youtube.utils.resourceid.playerCollapseButton
import app.morphe.patches.youtube.utils.resourceid.titleAnchor
import app.morphe.patches.youtube.utils.resourceid.youTubeControlsOverlaySubtitleButton
import app.morphe.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object FullScreenButtonFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        literal(fullScreenButton),
    ),
    custom = handler@{ method, _ ->
        if (!method.containsLiteralInstruction(fullScreenButton))
            return@handler false

        method.containsLiteralInstruction(fadeDurationFast) // YouTube 18.29.38 ~ YouTube 19.18.41
                || method.containsLiteralInstruction(cfFullscreenButton) // YouTube 19.19.39 ~
    },
)

internal const val LITHO_SUBTITLE_BUTTON_FEATURE_FLAG = 45421555L
internal const val CAST_BUTTON_PLAYER_FEATURE_FLAG = 45690091L
internal const val CAST_BUTTON_ACTION_FEATURE_FLAG = 45690090L

internal object MediaRouteButtonFingerprint : Fingerprint(
    definingClass = "/MediaRouteButton;",
    name = "setVisibility",
    parameters = listOf("I"),
)

internal object CastButtonPlayerFeatureFlagFingerprint : Fingerprint(
    returnType = "Z",
    filters = listOf(
        literal(CAST_BUTTON_PLAYER_FEATURE_FLAG),
    )
)

internal object CastButtonActionFeatureFlagFingerprint : Fingerprint(
    returnType = "Z",
    filters = listOf(
        literal(CAST_BUTTON_ACTION_FEATURE_FLAG),
    )
)

/**
 * Added in YouTube v18.31.40
 *
 * When this value is TRUE, litho subtitle button is used.
 * In this case, the empty area remains, so set this value to FALSE.
 */
internal object LithoSubtitleButtonConfigFingerprint : Fingerprint(
    filters = listOf(
        literal(LITHO_SUBTITLE_BUTTON_FEATURE_FLAG),
    )
)

internal object MusicAppDeeplinkButtonFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "Z")
)

internal object MusicAppDeeplinkButtonParentFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        literal(musicAppDeeplinkButtonView),
    )
)

internal object PlayerControlsVisibilityModelFingerprint : Fingerprint(
    filters = listOf(
        string("Missing required properties:"),
        string("hasNext"),
        string("hasPrevious"),
        opcode(Opcode.INVOKE_DIRECT_RANGE),
    )
)

internal object TitleAnchorFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        literal(playerCollapseButton),
        literal(titleAnchor),
    )
)

/**
 * The parameters of the method have changed in YouTube v18.31.40.
 * Therefore, this fingerprint does not check the method's parameters.
 *
 * This fingerprint is compatible from YouTube v18.25.40 to YouTube v18.45.43
 */
internal object YoutubeControlsOverlaySubtitleButtonFingerprint : Fingerprint(
    returnType = "L",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    filters = listOf(
        literal(youTubeControlsOverlaySubtitleButton),
    )
)
