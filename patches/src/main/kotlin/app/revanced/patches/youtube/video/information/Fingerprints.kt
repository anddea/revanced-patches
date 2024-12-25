package app.revanced.patches.youtube.video.information

import app.revanced.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.notificationBigPictureIconWidth
import app.revanced.patches.youtube.utils.resourceid.qualityAuto
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val channelIdFingerprint = legacyFingerprint(
    name = "channelIdFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("com.google.android.apps.youtube.mdx.watch.LAST_MEALBAR_PROMOTED_LIVE_FEED_CHANNELS")
)

internal val channelNameFingerprint = legacyFingerprint(
    name = "channelNameFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf(
        "setMetadata may only be called once",
        "Person",
    )
)

internal val onPlaybackSpeedItemClickFingerprint = legacyFingerprint(
    name = "onPlaybackSpeedItemClickFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Landroid/widget/AdapterView;", "Landroid/view/View;", "I", "J"),
    customFingerprint = { method, _ ->
        method.name == "onItemClick" &&
                method.indexOfFirstInstruction {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
                } >= 0
    }
)

internal val playbackInitializationFingerprint = legacyFingerprint(
    name = "playbackInitializationFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("play() called when the player wasn\'t loaded."),
    customFingerprint = { method, _ ->
        indexOfPlayerResponseModelDirectInstruction(method) >= 0
    }
)

internal fun indexOfPlayerResponseModelDirectInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_DIRECT &&
                getReference<MethodReference>()?.returnType == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
    }

internal val playbackSpeedClassFingerprint = legacyFingerprint(
    name = "playbackSpeedClassFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.RETURN_OBJECT),
    strings = listOf("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT")
)

internal val playerControllerSetTimeReferenceFingerprint = legacyFingerprint(
    name = "playerControllerSetTimeReferenceFingerprint",
    opcodes = listOf(
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.IGET_OBJECT
    ),
    strings = listOf("Media progress reported outside media playback: ")
)

internal val seekRelativeFingerprint = legacyFingerprint(
    name = "seekRelativeFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    // returnType = "Z", ~ YouTube 19.39.39
    // returnType = "V", YouTube 19.40.xx ~
    parameters = listOf("J", "L"),
    opcodes = listOf(
        Opcode.ADD_LONG_2ADDR,
        Opcode.INVOKE_VIRTUAL,
    )
)

internal val videoIdFingerprint = legacyFingerprint(
    name = "videoIdFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("Failed to download video (IllegalStateException): %s")
)

/**
 * Renamed from VideoIdWithoutShortsFingerprint
 */
internal val videoIdFingerprintBackgroundPlay = legacyFingerprint(
    name = "videoIdFingerprintBackgroundPlay",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.MONITOR_EXIT,
        Opcode.RETURN_VOID,
        Opcode.MONITOR_EXIT,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, classDef ->
        method.name == "l" &&
                classDef.methods.count() == 17 &&
                method.implementation != null &&
                indexOfPlayerResponseModelInterfaceInstruction(method) >= 0
    }
)

fun indexOfPlayerResponseModelInterfaceInstruction(methodDef: Method) =
    methodDef.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_INTERFACE &&
                getReference<MethodReference>()?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
    }

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

internal val videoQualityListFingerprint = legacyFingerprint(
    name = "videoQualityListFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    literals = listOf(qualityAuto),
)

internal val videoQualityTextFingerprint = legacyFingerprint(
    name = "videoQualityTextFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("[L", "I", "Z"),
    opcodes = listOf(
        Opcode.IF_GE,
        Opcode.AGET_OBJECT,
        Opcode.IGET_OBJECT
    ),
    strings = listOf("menu_item_video_quality")
)

internal val videoTitleFingerprint = legacyFingerprint(
    name = "videoTitleFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(notificationBigPictureIconWidth),
)
