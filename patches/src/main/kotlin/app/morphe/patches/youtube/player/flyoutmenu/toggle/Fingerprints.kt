package app.morphe.patches.youtube.player.flyoutmenu.toggle

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val additionalSettingsConfigFingerprint = legacyFingerprint(
    name = "additionalSettingsConfigFingerprint",
    returnType = "Z",
    literals = listOf(45412662L),
)

internal val cinematicLightingFingerprint = legacyFingerprint(
    name = "cinematicLightingFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("menu_item_cinematic_lighting")
)

internal val pipFingerprint = legacyFingerprint(
    name = "pipFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("menu_item_picture_in_picture"),
    customFingerprint = { _, classDef ->
        classDef.methods.count() > 5
    }
)

internal val playbackLoopInitFingerprint = legacyFingerprint(
    name = "playbackLoopInitFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("menu_item_single_video_playback_loop")
)

internal val playbackLoopOnClickListenerFingerprint = legacyFingerprint(
    name = "playbackLoopOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I", "Z"),
    strings = listOf("menu_item_single_video_playback_loop")
)

internal val stableVolumeFingerprint = legacyFingerprint(
    name = "stableVolumeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("menu_item_stable_volume")
)
