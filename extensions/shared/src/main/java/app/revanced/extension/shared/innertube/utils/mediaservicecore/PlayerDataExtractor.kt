package app.revanced.extension.shared.innertube.utils.mediaservicecore

import app.revanced.extension.shared.utils.Logger
import com.eclipsesource.v8.V8ScriptExecutionException

/**
 * Powered by [MediaServiceCore](https://github.com/yuliskov/MediaServiceCore/blob/f5691d30c81342548852c6951bc7ea5bb8a810ca/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/playerdata/PlayerDataExtractor.kt)
 */
internal class PlayerDataExtractor(val jsCode: String?) {
    private var mNFuncCode: Pair<List<String>, String>? = null
    private var mSigFuncCode: Pair<List<String>, String>? = null
    private var mNSigTmp: Pair<String, String?>? = null

    init {
        if (mNFuncCode == null || mSigFuncCode == null) {
            fetchAllData()
            checkAllData()
        }
    }

    fun extractNSig(nParam: String): String? {
        if (mNSigTmp?.first == nParam) return mNSigTmp?.second

        val nSig = extractNSigReal(nParam)

        mNSigTmp = Pair(nParam, nSig)

        return nSig
    }

    fun extractSig(signature: String): String? {
        val nSig = extractSigReal(signature)

        return nSig
    }

    private fun extractNSigReal(nParam: String): String? {
        val funcCode = mNFuncCode ?: return null

        val func = JSInterpret.extractFunctionFromCode(funcCode.first, funcCode.second)

        return func(listOf(nParam))
    }

    private fun extractSigReal(signature: String): String? {
        val funcCode = mSigFuncCode ?: return null

        val func = JSInterpret.extractFunctionFromCode(funcCode.first, funcCode.second)

        return func(listOf(signature))
    }

    private fun fetchAllData() {
        val globalVarData = jsCode?.let { CommonExtractor.extractPlayerJsGlobalVar(it) } ?: Triple(null, null, null)
        val globalVar = CommonExtractor.interpretPlayerJsGlobalVar(globalVarData)

        mNFuncCode = jsCode?.let {
            try {
                NSigExtractor.extractNFuncCode(it, globalVar)
            } catch (e: Throwable) {
                Logger.printException({ "NSig init failed" }, e )
                null
            }
        }
        mSigFuncCode = jsCode?.let {
            try {
                SigExtractor.extractSigCode(it, globalVar)
            } catch (e: Throwable) {
                Logger.printException({ "Signature init failed" }, e )
                null
            }
        }
    }

    private fun checkAllData() {
        mNFuncCode?.let {
            try {
                val param = "5cNpZqIJ7ixNqU68Y7S"
                val result = extractNSigReal(param)
                if (result == null || result == param)
                    mNFuncCode = null
            } catch (error: V8ScriptExecutionException) {
                Logger.printException({ "NSig check failed" }, error )
                mNFuncCode = null
            }
        }

        mSigFuncCode?.let {
            try {
                val param = "5cNpZqIJ7ixNqU68Y7S"
                val result = extractSigReal(param)
                if (result == null || result == param)
                    mSigFuncCode = null
            } catch (error: V8ScriptExecutionException) {
                Logger.printException({ "Signature check failed" }, error )
                mSigFuncCode = null
            }
        }
    }
}