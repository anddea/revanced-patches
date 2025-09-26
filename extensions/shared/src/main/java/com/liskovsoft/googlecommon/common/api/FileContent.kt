package com.liskovsoft.googlecommon.common.api

import com.liskovsoft.googlecommon.common.converters.regexp.RegExp

internal class FileContent {
    @RegExp("[\\w\\W]*")
    private val mContent: String? = null

    val content: String?
        get() = mContent
}
