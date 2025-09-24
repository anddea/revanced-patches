package com.liskovsoft.youtubeapi.app.playerdata

import app.revanced.extension.shared.utils.Logger

import com.eclipsesource.v8.V8ScriptExecutionException
import com.liskovsoft.googlecommon.common.js.JSInterpret
import java.util.regex.Pattern

/**
 * yt_dlp.extractor.youtube._video.YoutubeIE._parse_sig_js
 */
internal object SigExtractor {
    private val mSigPattern = Pattern.compile("""(?xs)
                \b([a-zA-Z0-9_$]+)&&\(\1=([a-zA-Z0-9_$]{2,})\(decodeURIComponent\(\1\)\)""", Pattern.COMMENTS)
    private val mSigPattern2 = Pattern.compile("""(?x)
                ;\w+\ [$\w]+=\{[\S\s]{10,200}?[\w]\.reverse\(\)[\S\s]*?
                function\ ([$\w]+)\(([\w])\)\{.*[\w]\.split\((?:""|[$\w]+\[\d+\])\).*;return\ [\w]\.join\((?:""|[$\w]+\[\d+\])\)\}""", Pattern.COMMENTS)

    /**
     * yt_dlp.extractor.youtube.YoutubeIE._extract_n_function_code
     *
     * yt-dlp\yt_dlp\extractor\youtube.py
     */
    fun extractSigCode(jsCode: String, globalVar: Triple<String?, List<String>?, String?>): Pair<List<String>, String>? {
        val funcName = extractSigFunctionName(jsCode) ?: return null

        //val funcCode = fixupSigFunctionCode(JSInterpret.extractFunctionCode(jsCode, funcName), globalVarData)

        return fixupGlobalObjIfNeeded(jsCode, JSInterpret.extractFunctionCode(jsCode, funcName), globalVar) ?: extractSigFunctionCodeAlt(jsCode, globalVar)
    }

    /**
     * yt_dlp.extractor.youtube._video.YoutubeIE._parse_sig_js
     */
    private fun extractSigFunctionName(jsCode: String): String? {
        val sigFuncMatcher = mSigPattern.matcher(jsCode)

        if (sigFuncMatcher.find() && sigFuncMatcher.groupCount() >= 2) {
            return sigFuncMatcher.group(2)
        }

        return null
    }

    private fun fixupSigFunctionCode(funcCode: Pair<List<String>, String>, globalVar: Triple<String?, List<String>?, String?>): Pair<List<String>, String> {
        val argNames = funcCode.first
        var sigCode = funcCode.second

        val (varName, globalList, varCode) = globalVar

        if (varName != null && globalList != null && varCode != null) {
            Logger.printDebug { "Prepending sig function code with global array variable \"$varName\"" }
            sigCode = "$varCode; $sigCode"
        }

        return Pair(argNames, sigCode)
    }

    private fun fixupGlobalObjIfNeeded(jsCode: String, funcCode: Pair<List<String>, String>, globalVar: Triple<String?, List<String>?, String?>, nestedCount: Int = 0): Pair<List<String>, String>? {
        var fixedFuncCode = fixupSigFunctionCode(funcCode, globalVar)

        // Test the function works
        try {
            extractSig(fixedFuncCode, "5cNpZqIJ7ixNqU68Y7S")
        } catch (error: V8ScriptExecutionException) {
            if (nestedCount > 1)
                return null

            val globalObjNamePattern = Pattern.compile("""([\w$]+) is not defined$""")

            val globalObjNameMatcher = globalObjNamePattern.matcher(error.message!!)

            if (globalObjNameMatcher.find() && globalObjNameMatcher.groupCount() == 1) {
                val globalObjCode = try {
                    JSInterpret.extractObjectCode(jsCode, globalObjNameMatcher.group(1)!!)
                } catch (e: Exception) {
                    return null
                }
                
                val (varName, globalList, varCode) = globalVar
                fixedFuncCode = fixupGlobalObjIfNeeded(
                    jsCode, funcCode, Triple(varName, globalList, "${varCode?.let { "$it;" } ?: ""} $globalObjCode"), nestedCount + 1) ?: return null
            }
        }

        return fixedFuncCode
    }

    private fun extractSigFunctionCodeAlt(jsCode: String, globalVar: Triple<String?, List<String>?, String?>): Pair<List<String>, String>? {
        val cipherMatcher = mSigPattern2.matcher(jsCode)

        if (cipherMatcher.find()) {
            val fnName: String = cipherMatcher.group(1)!!
            val fnParamName: String = cipherMatcher.group(2)!!
            val fnBody = "${cipherMatcher.group(0)!!}; return $fnName($fnParamName);"
            val varCode = globalVar.third
            return Pair(listOf(fnParamName), "${varCode?.let { "$it;" } ?: ""} $fnBody")
        }

        return null
    }

    private fun extractSig(funcCode: Pair<List<String>, String>, signature: String): String? {
        val func = JSInterpret.extractFunctionFromCode(funcCode.first, funcCode.second)

        return func(listOf(signature))
    }
}