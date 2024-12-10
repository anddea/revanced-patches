@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.general.miniplayer

import app.revanced.patches.youtube.utils.playservice.is_19_25_or_greater
import app.revanced.patches.youtube.utils.resourceid.floatyBarTopMargin
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerClose
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerExpand
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerForwardButton
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerRewindButton
import app.revanced.patches.youtube.utils.resourceid.scrimOverlay
import app.revanced.patches.youtube.utils.resourceid.ytOutlinePictureInPictureWhite
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.util.MethodUtil

internal val miniplayerDimensionsCalculatorParentFingerprint = legacyFingerprint(
    name = "miniplayerDimensionsCalculatorParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(floatyBarTopMargin),
)

internal val miniplayerModernAddViewListenerFingerprint = legacyFingerprint(
    name = "miniplayerModernAddViewListenerFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Landroid/view/View;")
)

internal val miniplayerModernCloseButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernCloseButtonFingerprint",
    returnType = "Landroid/widget/ImageView;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerClose),
)

private var constructorMethodCount = 0

internal fun isMultiConstructorMethod() = constructorMethodCount > 1

internal val miniplayerModernConstructorFingerprint = legacyFingerprint(
    name = "miniplayerModernConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("L"),
    literals = listOf(45623000L),
    customFingerprint = custom@{ method, classDef ->
        classDef.methods.forEach {
            if (MethodUtil.isConstructor(it)) constructorMethodCount += 1
        }

        if (!is_19_25_or_greater)
            return@custom true

        // Double tap action (Used in YouTube 19.25.39+).
        method.containsLiteralInstruction(45628823L)
                && method.containsLiteralInstruction(45630429L)
    }
)

internal val miniplayerModernDragAndDropFingerprint = legacyFingerprint(
    name = "miniplayerModernDragAndDropFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("L"),
    literals = listOf(45628752L),
)

internal val miniplayerModernEnabledFingerprint = legacyFingerprint(
    name = "miniplayerModernEnabledFingerprint",
    literals = listOf(45622882L),
)

internal val miniplayerModernExpandButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernExpandButtonFingerprint",
    returnType = "Landroid/widget/ImageView;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerExpand),
)

internal val miniplayerModernExpandCloseDrawablesFingerprint = legacyFingerprint(
    name = "miniplayerModernExpandCloseDrawablesFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(ytOutlinePictureInPictureWhite),
)

internal val miniplayerModernForwardButtonFingerprint = legacyFingerprint(
    name = "miniplayerModernForwardButtonFingerprint",
    returnType = "Landroid/widget/ImageView;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(modernMiniPlayerForwardButton),
)

internal val miniplayerModernOverlayViewFingerprint = legacyFingerprint(
    name = "miniplayerModernOverlayViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(scrimOverlay),
)

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

internal val miniplayerOverrideFingerprint = legacyFingerprint(
    name = "miniplayerOverrideFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
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
        Opcode.CHECK_CAST,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
    )
)

internal val youTubePlayerOverlaysLayoutFingerprint = legacyFingerprint(
    name = "youTubePlayerOverlaysLayoutFingerprint",
    customFingerprint = { _, classDef ->
        classDef.type == YOUTUBE_PLAYER_OVERLAYS_LAYOUT_CLASS_NAME
    }
)

internal const val YOUTUBE_PLAYER_OVERLAYS_LAYOUT_CLASS_NAME =
    "Lcom/google/android/apps/youtube/app/common/player/overlay/YouTubePlayerOverlaysLayout;"
