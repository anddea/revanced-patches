package com.liskovsoft.youtubeapi.app.potokennp2

internal class PoTokenException(message: String) : RuntimeException(message)

// to be thrown if the WebView provided by the system is broken
internal class BadWebViewException(message: String) : RuntimeException(message)

internal fun buildExceptionForJsError(error: String): Throwable {
    return if (error.contains("SyntaxError"))
        BadWebViewException(error)
    else
        PoTokenException(error)
}
