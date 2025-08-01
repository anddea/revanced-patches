package app.revanced.extension.shared.innertube.utils.mediaservicecore

import app.revanced.extension.shared.utils.Logger

import com.google.gson.Gson
import java.util.regex.Pattern

/**
 * Powered by [MediaServiceCore](https://github.com/yuliskov/MediaServiceCore/blob/f5691d30c81342548852c6951bc7ea5bb8a810ca/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/playerdata/CommonExtractor.kt)
 */
internal object CommonExtractor {
    private val mGlobalVarPattern: Pattern = Pattern.compile("""(?x)
            (["'])use\s+strict\1;\s*
            (
                var\s+([a-zA-Z0-9_$]+)\s*=\s*
                (
                    (["'])(?:(?!\5).|\\.)+\5
                    \.split\((["'])(?:(?!\6).)+\6\)
                    |\[\s*(?:(["'])(?:(?!\7).|\\.)*\7\s*,?\s*)+\]
                )
            )[;,]
        """, Pattern.COMMENTS)

    fun extractPlayerJsGlobalVar(jsCode: String): Triple<String?, String?, String?> {
        val matcher = mGlobalVarPattern.matcher(jsCode)

        return if (matcher.find()) {
            val varCode = matcher.group(2) // full expression. E.g. var tmp = "hello";
            val varName = matcher.group(3) // assigned var name. E.g. tmp
            val varValue = matcher.group(4) // right side of assignment. E.g. "hello"
            Triple(varCode, varName, varValue)
        } else {
            Logger.printDebug { "No global array variable found in player JS" }
            Triple(null, null, null)
        }
    }

    fun interpretPlayerJsGlobalVar(globalVarData: Triple<String?, String?, String?>): Triple<String?, List<String>?, String?> {
        val (_, varName, varValue) = globalVarData

        val globalList = varValue?.let { JSInterpret.interpretExpression(it) }

        var varCode: String? = null

        if (varName != null && globalList != null) {
            varCode = "var $varName=${Gson().toJson(globalList)}"
        }

        return Triple(varName, globalList, varCode)
    }
}