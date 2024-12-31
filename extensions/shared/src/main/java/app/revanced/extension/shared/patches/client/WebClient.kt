package app.revanced.extension.shared.patches.client

/**
 * Used to fetch video information.
 */
@Suppress("unused")
object WebClient {
    /**
     * This user agent does not require a PoToken in [ClientType.MWEB]
     * https://github.com/yt-dlp/yt-dlp/blob/0b6b7742c2e7f2a1fcb0b54ef3dd484bab404b3f/yt_dlp/extractor/youtube.py#L259
     */
    private const val USER_AGENT_SAFARI =
        "Mozilla/5.0 (iPad; CPU OS 16_7_10 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1,gzip(gfe)"

    enum class ClientType(
        /**
         * [YouTube client type](https://github.com/zerodytrash/YouTube-Internal-Clients?tab=readme-ov-file#clients)
         */
        val id: Int,
        /**
         * Client user-agent.
         */
        @JvmField
        val userAgent: String = USER_AGENT_SAFARI,
        /**
         * Client version.
         */
        @JvmField
        val clientVersion: String
    ) {
        MWEB(
            id = 2,
            clientVersion = "2.20241202.07.00"
        ),
        WEB_REMIX(
            id = 29,
            clientVersion = "1.20241127.01.00",
        );

        @JvmField
        val clientName: String = name
    }
}
