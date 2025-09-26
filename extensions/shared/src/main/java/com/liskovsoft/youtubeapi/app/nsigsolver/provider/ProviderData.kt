package com.liskovsoft.youtubeapi.app.nsigsolver.provider

internal enum class JsChallengeType(val value: String) {
    N("n"),
    SIG("sig");
}

internal data class JsChallengeRequest(
    val type: JsChallengeType,
    val input: ChallengeInput,
    val videoId: String? = null
)

internal data class ChallengeInput(
    val playerUrl: String,
    val challenges: MutableList<String> = mutableListOf()
)

internal data class ChallengeOutput(
    val results: MutableMap<String, String> = mutableMapOf()
)

internal data class JsChallengeProviderResponse(
    val request: JsChallengeRequest,
    val response: JsChallengeResponse? = null,
    val error: Exception? = null
)

internal data class JsChallengeResponse(
    val type: JsChallengeType,
    val output: ChallengeOutput
)
