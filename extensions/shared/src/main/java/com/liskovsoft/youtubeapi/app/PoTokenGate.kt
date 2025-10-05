package com.liskovsoft.youtubeapi.app

import com.liskovsoft.youtubeapi.app.potokennp2.PoTokenProviderImpl
import com.liskovsoft.youtubeapi.app.potokennp2.misc.PoTokenResult

internal object PoTokenGate {
    private var mNpPoToken: PoTokenResult? = null
    private var mCacheResetTimeMs: Long = -1

    @JvmStatic
    fun getContentPoToken(videoId: String): String? {
        if (!PoTokenProviderImpl.isPotSupported) return null

        if (mNpPoToken?.videoId == videoId && !PoTokenProviderImpl.isExpired) {
            return mNpPoToken?.playerRequestPoToken
        }

        mNpPoToken = PoTokenProviderImpl.getWebClientPoToken(videoId)

        return mNpPoToken?.playerRequestPoToken
    }

    @JvmStatic
    fun getSessionPoToken(videoId: String): String? {
        if (!PoTokenProviderImpl.isPotSupported) return null

        if (mNpPoToken?.videoId == videoId) {
            val streamingDataPoToken = mNpPoToken!!.streamingDataPoToken
            if (streamingDataPoToken != null) {
                mNpPoToken = null
                return streamingDataPoToken
            }
        }

        mNpPoToken = PoTokenProviderImpl.getWebClientPoToken(videoId)

        if (mNpPoToken != null) {
            val streamingDataPoToken = mNpPoToken!!.streamingDataPoToken
            if (streamingDataPoToken != null) {
                mNpPoToken = null
                return streamingDataPoToken
            }
        }

        return null
    }

    @JvmStatic
    fun updatePoToken() {
        if (PoTokenProviderImpl.isPotSupported) {
            //mNpPoToken = null // only refresh
            mNpPoToken = PoTokenProviderImpl.getWebClientPoToken("") // refresh and preload
        }
    }

    @JvmStatic
    fun getVisitorData(videoId: String): String? {
        if (!PoTokenProviderImpl.isPotSupported) return null

        if (mNpPoToken?.videoId == videoId && !PoTokenProviderImpl.isExpired) {
            return mNpPoToken?.visitorData
        }

        mNpPoToken = PoTokenProviderImpl.getWebClientPoToken(videoId)

        return mNpPoToken?.visitorData
    }

    @JvmStatic
    fun resetCache(): Boolean {
        if (System.currentTimeMillis() < mCacheResetTimeMs) {
            return false
        }

        if (PoTokenProviderImpl.isPotSupported) {
            mNpPoToken = null
        }

        mCacheResetTimeMs = System.currentTimeMillis() + 60_000

        return true
    }
}