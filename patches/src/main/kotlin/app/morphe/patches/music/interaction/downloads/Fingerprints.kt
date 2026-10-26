/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.patches.music.interaction.downloads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.newInstance
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

// resolve native playlist commands and the local-playlist repository across obfuscated clients.
// InOrderCommandMapping.tryResolve; the patch hooks its caller, the dispatcher that chooses the resolver.
internal val nativeDownloadsCommandResolverFingerprint = Fingerprint(
    returnType = "Z",
    parameters = listOf("L", "L", "Ljava/util/Map;"),
    strings = listOf("CommandResolver threw exception during resolution"),
)

internal val nativePlaylistDatabaseFingerprint = Fingerprint(
    name = "<init>",
    parameters = listOf("L"),
    strings = listOf("music_sideloaded_playlist_database"),
)

/**
 * Request of the 'ypc/get_offline_upsell' endpoint.
 */
internal object OfflineUpsellRequestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    filters = listOf(
        string("ypc/get_offline_upsell")
    )
)

/**
 * Handles the Download command of accounts that are offered Premium instead,
 * by showing a progress dialog and then the Premium upsell of the endpoint.
 */
internal fun offlineUpsellCommandFingerprint(offlineUpsellRequestType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        string("com.google.android.libraries.youtube.logging.interaction_logger"),
        newInstance(offlineUpsellRequestType)
    )
)
