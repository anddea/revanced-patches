package com.liskovsoft.youtubeapi.app.nsigsolver.common

import com.liskovsoft.googlecommon.common.api.FileApi
import com.liskovsoft.googlecommon.common.helpers.RetrofitHelper
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.IEContentProviderError
import kotlinx.coroutines.delay

internal abstract class InfoExtractor {
    private val fileApi = RetrofitHelper.create(FileApi::class.java)

    protected suspend fun downloadWebpage(url: String, tries: Int = 1, timeoutMs: Long = 1_000, errorMsg: String? = null): String {
        var tryCount = 0

        while (true) {
            try {
                val content = RetrofitHelper.get(fileApi.getContent(url))?.content
                return content ?: throw IEContentProviderError(errorMsg ?: "Empty content received for the $url")
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