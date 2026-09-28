package app.morphe.patches.youtube.layout.livering

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.youtube.utils.navigation.YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val clientSettingEndpointFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = listOf(), returnType = "L"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        string("PLAYBACK_START_DESCRIPTOR_MUTATOR", MatchAfterImmediately()),
        string("force_fullscreen"),
        string("VideoPresenterConstants.VIDEO_THUMBNAIL_BITMAP_KEY")
    )
)

internal object YouTubeActivityOnCreateFingerprint : Fingerprint(
    definingClass = YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

// 21.20+
internal object ShortsPlaybackIntentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "L",
        "Ljava/util/Map;",
        "J",
        "Ljava/lang/String;"
    ),
    filters = listOf(
        // None of these strings are unique.
        string("PLAYBACK_START_DESCRIPTOR_MUTATOR"),
        string("com.google.android.apps.youtube.app.endpoint.flags"),
        string("com.google.android.libraries.youtube.rendering.elements.sender_view")
    )
)

// 21.19 and lower
internal object ShortsPlaybackIntentFingerprintLegacy : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "L",
        "Ljava/util/Map;",
        "J",
        "Ljava/lang/String;"
    ),
    filters = listOf(
        // None of these strings are unique.
        string("com.google.android.apps.youtube.app.endpoint.flags"),
        string("ReelWatchFragmentArgs"),
        string("reels_fragment_descriptor")
    )
)
