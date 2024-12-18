package app.revanced.patches.youtube.player.buttons

import app.revanced.patches.youtube.utils.resourceid.cfFullscreenButton
import app.revanced.patches.youtube.utils.resourceid.fadeDurationFast
import app.revanced.patches.youtube.utils.resourceid.fullScreenButton
import app.revanced.patches.youtube.utils.resourceid.musicAppDeeplinkButtonView
import app.revanced.patches.youtube.utils.resourceid.playerCollapseButton
import app.revanced.patches.youtube.utils.resourceid.titleAnchor
import app.revanced.patches.youtube.utils.resourceid.youTubeControlsOverlaySubtitleButton
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val fullScreenButtonFingerprint = legacyFingerprint(
    name = "fullScreenButtonFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    customFingerprint = handler@{ method, _ ->
        if (!method.containsLiteralInstruction(fullScreenButton))
            return@handler false

        method.containsLiteralInstruction(fadeDurationFast) // YouTube 18.29.38 ~ YouTube 19.18.41
                || method.containsLiteralInstruction(cfFullscreenButton) // YouTube 19.19.39 ~
    },
)

/**
 * Added in YouTube v18.31.40
 *
 * When this value is TRUE, litho subtitle button is used.
 * In this case, the empty area remains, so set this value to FALSE.
 */
internal val lithoSubtitleButtonConfigFingerprint = legacyFingerprint(
    name = "lithoSubtitleButtonConfigFingerprint",
    returnType = "Z",
    literals = listOf(45421555L),
)

internal val musicAppDeeplinkButtonFingerprint = legacyFingerprint(
    name = "musicAppDeeplinkButtonFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z", "Z")
)

internal val musicAppDeeplinkButtonParentFingerprint = legacyFingerprint(
    name = "musicAppDeeplinkButtonParentFingerprint",
    returnType = "V",
    literals = listOf(musicAppDeeplinkButtonView),
)

internal val playerControlsVisibilityModelFingerprint = legacyFingerprint(
    name = "playerControlsVisibilityModelFingerprint",
    opcodes = listOf(Opcode.INVOKE_DIRECT_RANGE),
    strings = listOf("Missing required properties:", "hasNext", "hasPrevious")
)

internal val titleAnchorFingerprint = legacyFingerprint(
    name = "titleAnchorFingerprint",
    returnType = "V",
    literals = listOf(playerCollapseButton, titleAnchor),
)

/**
 * The parameters of the method have changed in YouTube v18.31.40.
 * Therefore, this fingerprint does not check the method's parameters.
 *
 * This fingerprint is compatible from YouTube v18.25.40 to YouTube v18.45.43
 */
internal val youtubeControlsOverlaySubtitleButtonFingerprint = legacyFingerprint(
    name = "youtubeControlsOverlaySubtitleButtonFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    literals = listOf(youTubeControlsOverlaySubtitleButton),
)