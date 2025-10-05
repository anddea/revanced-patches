package com.liskovsoft.youtubeapi.app.nsigsolver.runtime

import com.google.gson.reflect.TypeToken

internal val solverOutputType = object : TypeToken<SolverOutput>() {}.type

internal data class SolverOutput(
    val type: String,
    val error: String?,
    val preprocessed_player: String?,
    val responses: List<ResponseData>
)

internal data class ResponseData(
    val type: String,
    val error: String?,
    val data: Map<String, String>
)