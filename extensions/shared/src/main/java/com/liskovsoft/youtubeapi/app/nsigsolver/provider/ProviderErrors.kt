package com.liskovsoft.youtubeapi.app.nsigsolver.provider

internal open class IEContentProviderError(message: String, cause: Exception? = null): Exception(message, cause)

/**
 * Reject the JsChallengeRequest (cannot handle the request)
 */
internal class JsChallengeProviderRejectedRequest(message: String, cause: Exception? = null): IEContentProviderError(message, cause)

/**
 * An error occurred while solving the challenge
 */
internal class JsChallengeProviderError(message: String, cause: Exception? = null): IEContentProviderError(message, cause)