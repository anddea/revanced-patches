package com.liskovsoft.youtubeapi.app.nsigsolver.common

import kotlinx.coroutines.runBlocking

internal object YTInfoExtractor: InfoExtractor() {
    // TODO: implement caching to the file
    fun loadPlayer(playerUrl: String): String? = runBlocking {
        return@runBlocking downloadWebpage(playerUrl)
    }

    fun downloadWebpageWithRetries(url: String) = runBlocking {
        return@runBlocking downloadWebpage(url, tries = 3)
    }
}