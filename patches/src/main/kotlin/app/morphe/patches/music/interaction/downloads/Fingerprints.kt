/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.patches.music.interaction.downloads

import app.morphe.patcher.Fingerprint

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
