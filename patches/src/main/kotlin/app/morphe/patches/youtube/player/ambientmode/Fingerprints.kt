package app.morphe.patches.youtube.player.ambientmode

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val AMBIENT_MODE_IN_FULLSCREEN_FEATURE_FLAG = 45389368L

/**
 * The ambient mode controllers (flyout menu item and background) are created if any flag is on.
 */
internal val AMBIENT_MODE_FEATURE_FLAGS = listOf(
    45376186L,
    45372814L,
)

internal object AmbientModeFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        anyInstruction(
            *AMBIENT_MODE_FEATURE_FLAGS.map { literal(it) }.toTypedArray()
        ),
    )
)

/**
 * CinematicContainerRenderer, the ambient mode proto of the /next response.
 * The literal is its extension field number.
 */
internal object CinematicContainerRendererFingerprint : Fingerprint(
    name = "<clinit>",
    returnType = "V",
    filters = listOf(
        literal(455752576L),
    )
)

/**
 * Reads the presentation style (field 3) of a CinematicContainerRenderer, if it has one.
 */
internal fun cinematicContainerPresentationStyleFingerprint(rendererType: String) = Fingerprint(
    returnType = "L",
    parameters = listOf(rendererType),
    filters = listOf(
        // Has-bits field.
        fieldAccess(opcode = Opcode.IGET, definingClass = rendererType, type = "I"),
        opcode(Opcode.AND_INT_LIT8, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        // Presentation style field.
        fieldAccess(
            opcode = Opcode.IGET,
            definingClass = rendererType,
            type = "I",
            location = MatchAfterImmediately()
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("I"),
            location = MatchAfterImmediately()
        ),
    )
)

/**
 * Reads the config (field 4) of a CinematicContainerRenderer.
 */
internal fun cinematicContainerConfigFingerprint(rendererType: String) = Fingerprint(
    filters = listOf(
        methodCall(name = "get", parameters = listOf(), returnType = "Ljava/lang/Object;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        checkCast(rendererType, location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = rendererType,
            location = MatchAfterImmediately()
        ),
    )
)

/**
 * Reads the 'ambient mode on by default' value (field 11) of the CinematicContainerRenderer config,
 * used if the flyout menu toggle was never changed.
 */
internal fun cinematicContainerEnabledByDefaultFingerprint(configType: String) = Fingerprint(
    filters = listOf(
        // Has-bits field.
        fieldAccess(opcode = Opcode.IGET, definingClass = configType, type = "I"),
        anyInstruction(
            opcode(Opcode.AND_INT_LIT16), // 20.51 - 21.20
            opcode(Opcode.AND_INT_2ADDR), // 20.21, 21.21+
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            definingClass = configType,
            type = "Z",
            location = MatchAfterImmediately()
        ),
        methodCall(
            smali = "Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;",
            location = MatchAfterImmediately()
        ),
    )
)

/**
 * Holds the CinematicContainerRenderer of the current video and its loading state.
 */
internal object CinematicContainerRendererWithStateFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("RendererWithState{rendererLoadedState="),
    )
)

/**
 * Loading state enum of [CinematicContainerRendererWithStateFingerprint].
 * Other enums have the same constant names, so the type is required.
 */
internal fun cinematicContainerRendererLoadedStateFingerprint(stateType: String) = Fingerprint(
    definingClass = stateType,
    name = "<clinit>",
    returnType = "V",
    filters = listOf(
        string("WAITING"),
        fieldAccess(definingClass = stateType, type = stateType, opcode = Opcode.SPUT_OBJECT),
        string("LOADED"),
        fieldAccess(definingClass = stateType, type = stateType, opcode = Opcode.SPUT_OBJECT),
    )
)


internal val ambientModeInFullscreenFingerprint = Fingerprint(
    returnType = "V",
    filters = listOf(literal(AMBIENT_MODE_IN_FULLSCREEN_FEATURE_FLAG)),
)

internal val powerSaveModeBroadcastReceiverFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    strings = listOf("android.os.action.POWER_SAVE_MODE_CHANGED"),
    // There are two classes that inherit [BroadcastReceiver].
    // Check the method count to find the correct class.
    custom = { _, classDef ->
        classDef.superclass == "Landroid/content/BroadcastReceiver;" &&
                classDef.methods.count() == 2
    }
)

internal val powerSaveModeSyntheticFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("android.os.action.POWER_SAVE_MODE_CHANGED")
)

internal val setFullScreenBackgroundColorFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = listOf("Z", "I", "I", "I", "I"),
    custom = { method, classDef ->
        classDef.type.endsWith("/YouTubePlayerViewNotForReflection;")
                && method.name == "onLayout"
    },
)
