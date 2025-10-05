package com.liskovsoft.googlecommon.common.js

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.util.regex.Pattern

@Suppress(
    "SENSELESS_COMPARISON",
    "RegExpUnnecessaryNonCapturingGroup",
    "unused",
    "SameParameterValue"
)
internal object JSInterpret {
    private val MATCHING_PARENS = mapOf('(' to ')', '{' to '}', '[' to ']')
    private val QUOTES = setOf('\'', '"', '/')

    fun extractFunctionFromCode(argNames: List<String>, code: String): (List<String>) -> String? {
        return { args: List<String> ->
            val fullCode =
                "(function (${argNames.joinToString(separator = ",")}) { $code })(${args.joinToString(separator = ",", prefix = "'", postfix = "'")})"
            V8Runtime.instance().evaluateWithErrors(fullCode)
        }
    }

    /**
     * yt_dlp.jsinterp.JSInterpreter.extract_function_code
     *
     * yt-dlp\yt_dlp\jsinterp.py
     */
    fun extractFunctionCode(jsCode: String, funcName: String): Pair<List<String>, String> {
        val escapedFuncName = Pattern.quote(funcName)
        val pattern = Pattern.compile(
            """(?xs)
                (?:
                    function\s+$escapedFuncName|
                    [{;,]\s*$escapedFuncName\s*=\s*function|
                    (?:var|const|let)\s+$escapedFuncName\s*=\s*function
                )\s*
                \(([^)]*)\)\s*
                (\{.+\})
            """.trimIndent()
        )
        val matcher = pattern.matcher(jsCode)
        if (!matcher.find()) {
            throw IllegalStateException("Could not find JS function \"$funcName\"")
        }
        val args = matcher.group(1)?.split(",")?.map { it.trim() } ?: emptyList()
        val codeBlock = matcher.group(2) ?: ""
        val (code, _) = separateAtParen(codeBlock)
        return Pair(args, code)
    }

    fun interpretExpression(jsCode: String): List<String>? {
        val result = V8Runtime.instance().evaluate("JSON.stringify($jsCode)")

        val gson = Gson()
        val listType = object : TypeToken<List<String>>() {}.type

        val response: List<String>? = try {
            gson.fromJson(result, listType)
        } catch (e: JsonSyntaxException) {
            null
        }

        return response
    }

    /**
     * yt_dlp.jsinterp.JSInterpreter.extract_object
     */
    fun extractObjectCode(jsCode: String, objName: String): String {
        val escapedObjName = Pattern.quote(objName)

        val funcNameRegex = """(?:[a-zA-Z$0-9]+|"[a-zA-Z$0-9]+"|'[a-zA-Z$}0-9]+')"""

        val objPattern = Pattern.compile("""(?x)
                (?<![a-zA-Z$0-9.])$escapedObjName\s*=\s*\{\s*
                    (($funcNameRegex\s*:\s*function\s*\(.*?\)\s*\{.*?\}(?:,\s*)?)*)
                \}\s*;
                """, Pattern.COMMENTS)

        val objMatcher = objPattern.matcher(jsCode)

        if (!objMatcher.find()) {
            throw IllegalStateException("Could not find JS function \"$objName\"")
        }

        return objMatcher.group()
    }

    /**
     * yt_dlp.jsinterp.JSInterpreter._separate_at_paren
     */
    private fun separateAtParen(expr: String, delim: Char? = null): Pair<String, String> {
        val delimiter = delim ?: expr.firstOrNull()?.let { MATCHING_PARENS[it] }
        ?: throw IllegalStateException("No delimiter provided and expression is empty")

        //val separated = separate(expr, delimiter, 1).toList()
        val separated = separate2(expr, delimiter.toString(), 1).toList()
        //val separated = separateOld(expr, delimiter.toString(), 1).toList()
        if (separated.size < 2) {
            if (separated.size == 1) {
                // No matched paren. Probably, we should delete the last paren.
                return separated[0].substring(0, separated[0].length - 1).trim() to ""
            } else {
                throw IllegalStateException("No terminating paren $delimiter in expression $expr")
            }
        }
        //return separated[0].substring(1).trim() to separated[1].trim()
        return separated[0].substring(1, separated[0].length - 1).trim() to separated[1].trim()
    }

    /**
     * yt_dlp.jsinterp.JSInterpreter._separate
     */
    private fun separate(expr: String, delim: Char = ',', maxSplit: Int? = null): List<String> {
        val opChars = "+-*/%&|^=<>!,;{}:["
        if (expr.isEmpty()) return emptyList()

        val counters = MATCHING_PARENS.values.associateWith { 0 }.toMutableMap()
        val quotes = "\"'`/"
        var start = 0
        var splits = 0
        var pos = 0
        val delimLen = delim.toString().length - 1
        var inQuote: Char? = null
        var escaping = false
        var afterOp = true
        var inRegexCharGroup = false
        val result = mutableListOf<String>()

        for ((idx, char) in expr.withIndex()) {
            if (inQuote == null && MATCHING_PARENS.containsKey(char)) {
                counters[MATCHING_PARENS[char]!!] = counters[MATCHING_PARENS[char]!!]!! + 1
            } else if (inQuote == null && counters.containsKey(char)) {
                if (counters[char]!! > 0) {
                    counters[char] = counters[char]!! - 1
                }
            } else if (!escaping) {
                if (char in quotes && (inQuote == null || inQuote == char)) {
                    if (inQuote != null || afterOp || char != '/') {
                        inQuote = if (inQuote != null && !inRegexCharGroup) null else char
                    } else if (inQuote == '/' && (char == '[' || char == ']')) {
                        inRegexCharGroup = char == '['
                    }
                }
                escaping = !escaping && inQuote != null && char == '\\'
                val inUnaryOp = inQuote == null && !inRegexCharGroup && afterOp !in listOf(true, false) && char in "-+"
                afterOp = if (inQuote == null && char in opChars) char != null else if (char.isWhitespace()) afterOp else false

                if (char != delim || counters.values.any { it > 0 } || inQuote != null || inUnaryOp) {
                    pos = 0
                    continue
                } else if (pos != delimLen) {
                    pos += 1
                    continue
                }
                result.add(expr.substring(start, idx - delimLen))
                start = idx + 1
                pos = 0
                splits += 1
                if (maxSplit != null && splits >= maxSplit) {
                    break
                }
            }
        }
        result.add(expr.substring(start))
        return result
    }

    /**
     * yt_dlp.jsinterp.JSInterpreter._separate
     */
    private fun separateOld(expr: String, delim: String = ",", maxSplit: Int? = null): List<String> {
        val opChars = setOf('+', '-', '*', '/', '%', '&', '|', '^', '=', '<', '>', '!', ',', ';', '{', '}', ':', '[')
        if (expr.isEmpty()) {
            return emptyList()
        }

        val counters =
            mutableMapOf<Char, Int>().apply { putAll(MATCHING_PARENS.values.map { it to 0 }) }

        var start = 0
        var splits = 0
        var pos = 0
        val delimLen = delim.length - 1
        var inQuote: Char? = null
        var escaping = false
        var afterOp = true
        var inRegexCharGroup = false

        val result = mutableListOf<String>()

        for ((idx, char) in expr.withIndex()) {
            if (inQuote == null && char in MATCHING_PARENS) {
                counters[MATCHING_PARENS[char]!!] = (counters[MATCHING_PARENS[char]!!] ?: 0) + 1
            } else if (inQuote == null && char in counters) {
                if (counters[char]!! > 0) {
                    counters[char] = (counters[char] ?: 0) - 1
                }
            } else if (!escaping) {
                if (char == '"' || char == '\'') {
                    if (inQuote != null && inQuote == char) {
                        inQuote = null
                    } else if (inQuote == null && (afterOp || char != '/')) {
                        inQuote = char
                    }
                } else if (inQuote == '/' && char in listOf('[', ']')) {
                    inRegexCharGroup = char == '['
                }
            }
            escaping = !escaping && inQuote != null && char == '\\'
            val inUnaryOp =
                (inQuote == null && !inRegexCharGroup && afterOp && char in setOf('-', '+'))
            afterOp =
                if (inQuote == null && char in opChars) true else (char.isWhitespace() && afterOp)  // ??? apfterOp

            if (char != delim[pos] || counters.values.any { it != 0 } || inQuote != null || inUnaryOp) {
                pos = 0
            } else if (pos != delimLen) {
                pos++
            } else {
                result.add(expr.substring(start, idx - delimLen))
                start = idx + 1
                pos = 0
                splits++
                if (maxSplit != null && splits >= maxSplit) {
                    break
                }
            }
        }
        result.add(expr.substring(start))
        return result
    }

    private fun separate2(
        expr: String,
        delim: String = ",",
        maxSplit: Int? = null
    ): Sequence<String> = sequence {
        if (expr.isEmpty()) return@sequence

        val OP_CHARS = "+-*/%&|^=<>!,;{}:["
        val delimLen = delim.length - 1
        val counters = MATCHING_PARENS.values.associateWith { 0 }.toMutableMap()
        var start = 0
        var splits = 0
        var pos = 0
        var inQuote: Char? = null
        var escaping = false
        var afterOp: Any? = true
        var inRegexCharGroup = false

        for ((idx, char) in expr.withIndex()) {
            if (inQuote == null && MATCHING_PARENS.containsKey(char)) {
                val close = MATCHING_PARENS[char]
                counters[close!!] = (counters[close] ?: 0) + 1
            } else if (inQuote == null && counters.containsKey(char)) {
                if (counters[char]!! > 0) counters[char] = counters[char]!! - 1
            } else if (!escaping) {
                if (QUOTES.contains(char) && (inQuote == char || inQuote == null)) {
                    if (inQuote != null || afterOp != false || char != '/') {
                        inQuote = if (inQuote != null && !inRegexCharGroup) null else char
                    }
                } else if (inQuote == '/' && (char == '[' || char == ']')) {
                    inRegexCharGroup = char == '['
                }
            }

            escaping = !escaping && inQuote != null && char == '\\'
            val inUnaryOp =
                (inQuote == null && !inRegexCharGroup && afterOp !is Boolean && char in "-+")
            afterOp =
                if (inQuote == null && OP_CHARS.contains(char)) char else if (char.isWhitespace()) afterOp else false

            if (char != delim[pos] || counters.values.any { it > 0 } || inQuote != null || inUnaryOp) {
                pos = 0
                continue
            } else if (pos != delimLen) {
                pos += 1
                continue
            }

            yield(expr.substring(start, idx - delimLen + 1))
            start = idx + 1
            pos = 0
            splits += 1
            if (maxSplit != null && splits >= maxSplit) break
        }

        yield(expr.substring(start))
    }

    fun searchJson(
        startPattern: Pattern, content: String, endPattern: Pattern = Pattern.compile(";"),
        containsPattern: Pattern = Pattern.compile("""\{(?s:.+?)\}""")
    ): String? {
        val jsonRegex =
            Pattern.compile("""(?:$startPattern)\s*($containsPattern)\s*(?:$endPattern)""")
        val matcher = jsonRegex.matcher(content)

        if (matcher.find() && matcher.groupCount() == 1) {
            return matcher.group(1)
        }

        return null
    }
}