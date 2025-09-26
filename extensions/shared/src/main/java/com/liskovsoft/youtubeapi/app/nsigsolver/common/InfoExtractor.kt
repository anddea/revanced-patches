package com.liskovsoft.youtubeapi.app.nsigsolver.common

import com.liskovsoft.googlecommon.common.api.FileApi
import com.liskovsoft.googlecommon.common.helpers.RetrofitHelper
import kotlinx.coroutines.delay

internal abstract class InfoExtractor {
    private val fileApi = RetrofitHelper.create(FileApi::class.java)
    
    protected suspend fun downloadWebpage(url: String, tries: Int = 1, timeoutMs: Long = 1_000): String? {
        var tryCount = 0

        while (true) {
            try {
               return RetrofitHelper.get(fileApi.getContent(url))?.content
            } catch (e: Exception) {
                tryCount++
                if (tryCount >= tries)
                    throw e
                if (timeoutMs > 0)
                    delay(timeoutMs)
            }
        }
    }
}