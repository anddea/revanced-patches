package com.liskovsoft.youtubeapi.app.potokennp2.visitor.data

internal data class VisitorResult(val responseContext: ResponseContext?) {
    data class ResponseContext(val visitorData: String?)
}
