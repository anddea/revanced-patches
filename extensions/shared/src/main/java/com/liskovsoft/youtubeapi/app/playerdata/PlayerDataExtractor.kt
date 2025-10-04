package com.liskovsoft.youtubeapi.app.playerdata

import app.revanced.extension.shared.utils.Logger
import com.eclipsesource.v8.V8ScriptExecutionException
import com.liskovsoft.googlecommon.common.js.JSInterpret
import com.liskovsoft.youtubeapi.app.nsigsolver.impl.V8ChallengeProvider
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.ChallengeInput
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeRequest
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.JsChallengeType

internal class PlayerDataExtractor(val playerJS: String, val ejs: Boolean) {
    private var mNFuncCode: Pair<List<String>, String>? = null
    private var mSigFuncCode: Pair<List<String>, String>? = null

    private var nSigTmp: Pair<String, String?>? = null
    private var initialized: Boolean = false

    init {
        if (!initialized) {
            initialized = true

            if (ejs) {
                V8ChallengeProvider.setPlayerJS(playerJS)
            }

            fetchAllData()
            checkAllData()
        }
    }

    fun extractNSig(nParam: String): String? {
        if (nSigTmp?.first == nParam) return nSigTmp?.second

        val nSig = extractNSigReal(nParam)

        nSigTmp = Pair(nParam, nSig)

        return nSig
    }

    fun extractSig(sParam: String): String? {
        if (ejs) {
            val result = V8ChallengeProvider.bulkSolve(
                listOf(JsChallengeRequest(JsChallengeType.SIG, ChallengeInput(sParam)))
            )

            return result.toList().firstOrNull()?.response?.output?.results?.values?.toList()
                ?.firstOrNull()
        } else {
            val funcCode = mSigFuncCode ?: return null

            val func = JSInterpret.extractFunctionFromCode(funcCode.first, funcCode.second)

            return func(listOf(sParam))
        }
    }

    private fun extractNSigReal(nParam: String): String? {
        if (ejs) {
            val result = V8ChallengeProvider.bulkSolve(
                listOf(JsChallengeRequest(JsChallengeType.N, ChallengeInput(nParam)))
            )

            return result.toList().firstOrNull()?.response?.output?.results?.get(nParam)
        } else {
            val funcCode = mNFuncCode ?: return null

            val func = JSInterpret.extractFunctionFromCode(funcCode.first, funcCode.second)

            return func(listOf(nParam))
        }
    }

    private fun fetchAllData() {
        if (ejs) return

        try {
            val globalVarData = CommonExtractor.extractPlayerJsGlobalVar(playerJS)
            val globalVar = CommonExtractor.interpretPlayerJsGlobalVar(globalVarData)

            mNFuncCode = try {
                NSigExtractor.extractNFuncCode(playerJS, globalVar)
            } catch (e: Throwable) {
                Logger.printException({ "NSig init failed" }, e)
                null
            }
            mSigFuncCode = try {
                SigExtractor.extractSigCode(playerJS, globalVar)
            } catch (e: Throwable) {
                Logger.printException({ "Signature init failed" }, e)
                null
            }
        } catch (e: Throwable) {
            Logger.printException({ "fetchAllData failed" }, e)
            null
        }
    }

    private fun checkAllData() {
        val param = "5cNpZqIJ7ixNqU68Y7S"
        if (ejs) {
            try {
                V8ChallengeProvider.bulkSolve(
                    listOf(
                        JsChallengeRequest(JsChallengeType.N, ChallengeInput(param)),
                        JsChallengeRequest(JsChallengeType.SIG, ChallengeInput(param)),
                    )
                )
            } catch (e: Exception) {
                Logger.printException({ "checkAllData failed" }, e)
            }
        } else {
            mNFuncCode?.let {
                try {
                    val result = extractNSigReal(param)
                    if (result == null || result == param)
                        mNFuncCode = null
                } catch (error: V8ScriptExecutionException) {
                    Logger.printException({ "NSig check failed" }, error)
                    mNFuncCode = null
                }
            }

            mSigFuncCode?.let {
                try {
                    val result = extractSig(param)
                    if (result == null || result == param)
                        mSigFuncCode = null
                } catch (error: V8ScriptExecutionException) {
                    Logger.printException({ "Signature check failed" }, error)
                    mSigFuncCode = null
                }
            }
        }
    }
}