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
        "host: m.youtube.com",
        "origin: https://m.youtube.com",
        "referer: https://m.youtube.com",
        "user-agent: Mozilla/5.0 (PlayStation Vita 3.74) AppleWebKit/537.73 (KHTML, like Gecko) Silk/3.2",
        "x-youtube-client-name: 2",
        "x-youtube-client-version: 2.20250812.01.00"
        )
    @POST("https://m.youtube.com/youtubei/v1/visitor_id")
    fun getVisitorId(@Body query: String = VisitorApiHelper.getVisitorQuery()): Call<VisitorResult?>
}
