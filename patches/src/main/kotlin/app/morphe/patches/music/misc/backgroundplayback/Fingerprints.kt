package app.morphe.patches.music.misc.backgroundplayback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val backgroundPlaybackManagerFingerprint = legacyFingerprint(
    name = "backgroundPlaybackManagerFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    literals = listOf(64657230L),
)

internal val dataSavingSettingsFragmentFingerprint = legacyFingerprint(
    name = "dataSavingSettingsFragmentFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
    strings = listOf("pref_key_dont_play_nma_video"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/DataSavingSettingsFragment;") &&
                method.name == "onCreatePreferences"
    }
)

/**
 * Matches the kids playback policy by its stable feature flag.
 *
 * The surrounding opcode sequence changed in YouTube Music 8.51, while the flag and method
 * contract remained stable.
 */
internal object KidsBackgroundPlaybackPolicyControllerFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I", "L", "Z"),
    filters = listOf(
        literal(45638079L)
    )
)

internal val musicBrowserServiceFingerprint = legacyFingerprint(
    name = "musicBrowserServiceFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
    strings = listOf("android.service.media.extra.RECENT"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/MusicBrowserService;")
    },
)

internal val podCastConfigFingerprint = legacyFingerprint(
    name = "podCastConfigFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(45388403L),
)
