package com.liskovsoft.youtubeapi.app.nsigsolver.common

internal open class ScriptLoaderError(message: String, cause: Exception? = null): Exception(message, cause)

internal fun loadScript(filename: String, errorMsg: String? = null): String {
    TODO("Not implemented")
}

internal fun loadScript(filename: List<String>, errorMsg: String? = null): String {
    TODO("Not implemented")
}