package com.liskovsoft.youtubeapi.app.nsigsolver.common

internal data class CachedData(
    val code: String,
    val version: String = "UNKNOWN",
    val variant: String = "UNKNOWN"
)

internal class Cache {
    fun load(section: String, key: String): CachedData? {
        // TODO: not implemented
        return null
    }

    fun store(section: String, key: String, content: CachedData) {
        // TODO: not implemented
    }
}
