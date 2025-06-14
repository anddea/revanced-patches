package app.revanced.patches.youtube.utils

import app.revanced.patcher.fingerprint
import app.revanced.patches.youtube.player.components.playerComponentsPatch
import app.revanced.patches.youtube.utils.resourceid.*
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
