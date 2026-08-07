/*
 * Copyright (C) 2025-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.shared.innertube.requests

import app.morphe.extension.shared.requests.Route
import app.morphe.extension.shared.requests.Route.CompiledRoute

object InnerTubeRoutes {

    @JvmField
    val CREATE_PLAYLIST = compileRoute(
        endpoint = "playlist/create",
    )

    @JvmField
    val DELETE_PLAYLIST = compileRoute(
        endpoint = "playlist/delete",
    )

    @JvmField
    val EDIT_PLAYLIST = compileRoute(
        endpoint = "browse/edit_playlist",
        params = arrayOf("fields=status," + "playlistEditResults"),
    )

    @JvmField
    val GET_CATEGORY = compileRoute(
        endpoint = "player",
        params = arrayOf("fields=microformat.playerMicroformatRenderer.category"),
    )

    @JvmField
    val GET_PLAYLISTS = compileRoute(
        endpoint = "playlist/get_add_to_playlist",
        params = arrayOf("fields=contents.addToPlaylistRenderer.playlists.playlistAddToOptionRenderer"),
    )

    @JvmField
    val GET_SET_VIDEO_ID = compileRoute(
        endpoint = "next",
        params = arrayOf(
            "fields=contents.singleColumnWatchNextResults." +
                    "playlist.playlist.contents.playlistPanelVideoRenderer." +
                    "playlistSetVideoId"
        ),
    )

    @JvmField
    val GET_PLAYLIST_ENDPOINT = compileRoute(
        endpoint = "next",
        params = arrayOf(
            "fields=contents.singleColumnWatchNextResults." +
                    "playlist.playlist.contents.playlistPanelVideoRenderer." +
                    "navigationEndpoint"
        ),
    )

    @JvmField
    val GET_PLAYLIST_PAGE = compileRoute(
        endpoint = "next",
        params = arrayOf("fields=contents.singleColumnWatchNextResults.playlist.playlist"),
    )

    @JvmField
    val GET_VIDEO_ACTION_BUTTON = compileRoute(
        endpoint = "next",
        params = arrayOf(
            "fields=contents.singleColumnWatchNextResults." +
                    "results.results.contents.slimVideoMetadataSectionRenderer." +
                    "contents.elementRenderer.newElement.type.componentType." +
                    "model.videoActionBarModel.videoActionBarData.buttons." +
                    "buttonViewModel"
        )
    )

    @JvmField
    val GET_VIDEO_DETAILS = compileRoute(
        endpoint = "player",
        params = arrayOf("fields=videoDetails")
    )

    fun getStreamingDataRoute(
        tParameter: String,
        isInlinePlayback: Boolean = false,
    ): CompiledRoute =
        compileRoute(
            endpoint = "player",
            params = if (isInlinePlayback)
                arrayOf(
                    "fields=playabilityStatus.status,streamingData",
                    "t=$tParameter",
                    "inline=1",
                    "alt=proto"
                )
            else
                arrayOf(
                    "fields=playabilityStatus.status,streamingData",
                    "t=$tParameter",
                    "alt=proto"
                ),
        )

    private fun compileRoute(
        endpoint: String,
        prettier: Boolean = false,
        vararg params: String,
    ): CompiledRoute {
        val sb = StringBuilder(endpoint)
        val fieldParams = params.toMutableList()
        if (!prettier) {
            fieldParams += listOf("prettyPrint=false")
        }
        for (i in fieldParams.indices) {
            val query = if (i == 0) "?" else "&"
            sb.append(query)
            sb.append(fieldParams[i])
        }
        return Route(
            Route.Method.POST,
            sb.toString()
        ).compile()
    }

}
