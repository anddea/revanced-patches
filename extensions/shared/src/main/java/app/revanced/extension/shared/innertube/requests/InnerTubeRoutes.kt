package app.revanced.extension.shared.innertube.requests

import app.revanced.extension.shared.requests.Route
import app.revanced.extension.shared.requests.Route.CompiledRoute

object InnerTubeRoutes {

    @JvmField
    val CREATE_PLAYLIST = compileRoute(
        endpoint = "playlist/create",
        fields = "playlistId",
    )

    @JvmField
    val DELETE_PLAYLIST = compileRoute(
        endpoint = "playlist/delete",
    )

    @JvmField
    val EDIT_PLAYLIST = compileRoute(
        endpoint = "browse/edit_playlist",
        fields = "status," + "playlistEditResults",
    )

    @JvmField
    val GET_CATEGORY = compileRoute(
        endpoint = "player",
        fields = "microformat.playerMicroformatRenderer.category",
    )

    @JvmField
    val GET_PLAYLISTS = compileRoute(
        endpoint = "playlist/get_add_to_playlist",
        fields = "contents.addToPlaylistRenderer.playlists.playlistAddToOptionRenderer",
    )

    @JvmField
    val GET_SET_VIDEO_ID = compileRoute(
        endpoint = "next",
        fields = "contents.singleColumnWatchNextResults." +
                "playlist.playlist.contents.playlistPanelVideoRenderer." +
                "playlistSetVideoId",
    )

    @JvmField
    val GET_PLAYLIST_PAGE = compileRoute(
        endpoint = "next",
        fields = "contents.singleColumnWatchNextResults.playlist.playlist",
    )

    @JvmField
    val GET_STREAMING_DATA = compileRoute(
        endpoint = "player",
        fields = "streamingData",
        alt = "proto",
        prettier = true,
    )

    @JvmField
    val GET_STREAMING_DATA_JSON = compileRoute(
        endpoint = "player",
        fields = "streamingData.adaptiveFormats.audioTrack"
    )

    @JvmField
    val GET_VIDEO_ACTION_BUTTON = compileRoute(
        endpoint = "next",
        fields = "contents.singleColumnWatchNextResults." +
                "results.results.contents.slimVideoMetadataSectionRenderer." +
                "contents.elementRenderer.newElement.type.componentType." +
                "model.videoActionBarModel.buttons.buttonViewModel"
    )

    @JvmField
    val GET_VIDEO_DETAILS = compileRoute(
        endpoint = "player",
        fields = "videoDetails.channelId," +
                "videoDetails.isLiveContent," +
                "videoDetails.isUpcoming"
    )

    private fun compileRoute(
        endpoint: String,
        fields: String? = null,
        alt: String? = null,
        prettier: Boolean = false,
    ): CompiledRoute {
        val query = Array(4) { "&" }
        var i = 0
        query[i] = "?"

        val sb = StringBuilder(endpoint)
        if (!prettier) {
            sb.append(query[i++])
            sb.append("prettyPrint=false")
        }
        if (fields != null) {
            sb.append(query[i++])
            sb.append("fields=")
            sb.append(fields)
        }
        if (alt != null) {
            sb.append(query[i++])
            sb.append("alt=")
            sb.append(alt)
        }

        return Route(
            Route.Method.POST,
            sb.toString()
        ).compile()
    }

}