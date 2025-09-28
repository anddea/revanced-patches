package com.liskovsoft.youtubeapi.app.nsigsolver.common

import kotlinx.coroutines.runBlocking

internal object YouTubeInfoExtractor: InfoExtractor() {
    val cache: Cache = Cache()

    // TODO: implement caching to the local storage
    fun loadPlayer(playerUrl: String): String = runBlocking {
        return@runBlocking downloadWebpage(playerUrl)
    }

    fun downloadWebpageWithRetries(url: String, errorMsg: String? = null): String = runBlocking {
        return@runBlocking downloadWebpage(url, tries = 3, errorMsg = errorMsg)
    }
}