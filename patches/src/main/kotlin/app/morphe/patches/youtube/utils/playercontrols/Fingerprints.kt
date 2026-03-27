package app.morphe.patches.youtube.utils.playercontrols

import app.morphe.patches.youtube.utils.resourceid.bottomUiContainerStub
import app.morphe.patches.youtube.utils.resourceid.controlsLayoutStub
import app.morphe.patches.youtube.utils.resourceid.fullScreenButton
import app.morphe.patches.youtube.utils.resourceid.heatseekerViewstub
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val playerControlsVisibilityEntityModelFingerprint = legacyFingerprint(
    name = "playerControlsVisibilityEntityModelFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC.value,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.INVOKE_STATIC
    ),
    customFingerprint = { method, _ -> method.name == "getPlayerControlsVisibility" }
)

internal val playerTopControlsInflateFingerprint = legacyFingerprint(
    name = "playerTopControlsInflateFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = emptyList(),
    literals = listOf(controlsLayoutStub)
)

internal val playerControlsExtensionHookListenersExistFingerprint = legacyFingerprint(
    name = "playerControlsExtensionHookListenersExistFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    returnType = "Z",
    parameters = emptyList(),
    customFingerprint = { method, classDef ->
        method.name == "fullscreenButtonVisibilityCallbacksExist" &&
                classDef.type == EXTENSION_CLASS_DESCRIPTOR
    }
)

internal val playerControlsExtensionHookFingerprint = legacyFingerprint(
    name = "playerControlsExtensionHookFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    returnType = "V",
    parameters = listOf("Z"),
    customFingerprint = { method, classDef ->
        method.name == "fullscreenButtonVisibilityChanged" &&
                classDef.type == EXTENSION_CLASS_DESCRIPTOR
    }
)

internal val playerBottomControlsInflateFingerprint = legacyFingerprint(
    name = "playerBottomControlsInflateFingerprint",
    returnType = "Ljava/lang/Object;",
    parameters = emptyList(),
    literals = listOf(bottomUiContainerStub)
)

internal val overlayViewInflateFingerprint = legacyFingerprint(
    name = "overlayViewInflateFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    literals = listOf(fullScreenButton, heatseekerViewstub)
)

/**
 * Resolves to the class found in [playerTopControlsInflateFingerprint].
 */
internal val controlsOverlayVisibilityFingerprint = legacyFingerprint(
    name = "controlsOverlayVisibilityFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Z", "Z")
)

internal val motionEventFingerprint = legacyFingerprint(
    name = "motionEventFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/view/MotionEvent;"),
    customFingerprint = { method, _ ->
        indexOfTranslationInstruction(method) >= 0
    }
)

internal fun indexOfTranslationInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        getReference<MethodReference>()?.name == "setTranslationY"
    }

internal const val PLAYER_BOTTOM_CONTROLS_EXPLODER_FEATURE_FLAG = 45643739L

internal val playerBottomControlsExploderFeatureFlagFingerprint = legacyFingerprint(
    name = "playerBottomControlsExploderFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(PLAYER_BOTTOM_CONTROLS_EXPLODER_FEATURE_FLAG),
)

internal const val PLAYER_TOP_CONTROLS_EXPERIMENTAL_LAYOUT_FEATURE_FLAG = 45629424L

internal val playerTopControlsExperimentalLayoutFeatureFlagFingerprint = legacyFingerprint(
    name = "playerTopControlsExperimentalLayoutFeatureFlagFingerprint",
    returnType = "I",
    literals = listOf(PLAYER_TOP_CONTROLS_EXPERIMENTAL_LAYOUT_FEATURE_FLAG),
)

internal const val PLAYER_CONTROLS_FULLSCREEN_LARGE_BUTTON_FEATURE_FLAG = 45686474L

internal val playerControlsFullscreenLargeButtonsFeatureFlagFingerprint = legacyFingerprint(
    name = "playerControlsFullscreenLargeButtonsFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(PLAYER_CONTROLS_FULLSCREEN_LARGE_BUTTON_FEATURE_FLAG),
)

internal const val PLAYER_CONTROLS_FULLSCREEN_LARGE_OVERLAY_BUTTON_FEATURE_FLAG = 45709810L

internal val playerControlsLargeOverlayButtonsFeatureFlagFingerprint = legacyFingerprint(
    name = "playerControlsLargeOverlayButtonsFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(PLAYER_CONTROLS_FULLSCREEN_LARGE_OVERLAY_BUTTON_FEATURE_FLAG),
)

internal const val PLAYER_CONTROLS_BUTTON_STROKE_FEATURE_FLAG = 45713296L

internal val playerControlsButtonStrokeFeatureFlagFingerprint = legacyFingerprint(
    name = "playerControlsButtonStrokeFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(PLAYER_CONTROLS_BUTTON_STROKE_FEATURE_FLAG),
)