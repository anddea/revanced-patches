package com.liskovsoft.youtubeapi.app.nsigsolver.runtime

import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeProvider
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeType

internal abstract class JsRuntimeChalBaseJCP: JsChallengeProvider() {
    private val jcpGuideUrl = "https://github.com/yt-dlp/yt-dlp/wiki/YouTube-JS-Challenges"
    private val repository = "yt-dlp/yt-dlp-jsc-deno"
    override val supportedTypes = listOf(JsChallengeType.N, JsChallengeType.SIG)

    
}