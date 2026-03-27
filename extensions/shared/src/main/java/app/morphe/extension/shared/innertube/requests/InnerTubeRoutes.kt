package app.morphe.extension.shared.innertube.requests

import app.morphe.extension.shared.requests.Route
import app.morphe.extension.shared.requests.Route.CompiledRoute

object InnerTubeRoutes {

    @JvmField
    val CREATE_PLAYLIST = compileRoute(
        endpoint = "playlist/create",
        params = arrayOf("fields=playlistId"),
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
        for (i in 0 until fieldParams.size) {
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
