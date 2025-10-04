package com.liskovsoft.youtubeapi.app.potokennp2.visitor

import com.liskovsoft.googlecommon.common.converters.gson.WithGson
import com.liskovsoft.youtubeapi.app.potokennp2.visitor.data.VisitorResult
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

@WithGson
internal interface VisitorApi {
    @Headers(
        "Content-Type: application/json",
        "accept-language: en-US, en;q=0.9",
        "cookie: SOCS=CAE=",
        "host: www.youtube.com",
        "origin: https://www.youtube.com",
        "referer: https://www.youtube.com",
        "user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0",
        "x-youtube-client-name: 1",
        "x-youtube-client-version: 2.20250213.05.00"
    )
    @POST("https://www.youtube.com/youtubei/v1/visitor_id")
    fun getVisitorId(@Body query: String = VisitorApiHelper.getVisitorQuery()): Call<VisitorResult?>
}
