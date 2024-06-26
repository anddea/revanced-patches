package app.revanced.patches.youtube.video.playerresponse.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.video.playerresponse.fingerprint.PlayerParameterBuilderFingerprint.ENDS_WITH_PARAMETER_LIST
import app.revanced.patches.youtube.video.playerresponse.fingerprint.PlayerParameterBuilderFingerprint.STARTS_WITH_PARAMETER_LIST
import app.revanced.patches.youtube.video.playerresponse.fingerprint.PlayerParameterBuilderFingerprint.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlayerParameterBuilderFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    // 19.22 and earlier parameters are:
    // "Ljava/lang/String;", // VideoId.
    // "[B",
    // "Ljava/lang/String;", // Player parameters proto buffer.
    // "Ljava/lang/String;", // PlaylistId.
    // "I",
    // "I",
    // "Ljava/util/Set;",
    // "Ljava/lang/String;",
    // "Ljava/lang/String;",
    // "L",
    // "Z", // Appears to indicate if the video id is being opened or is currently playing.
    // "Z",
    // "Z"

    // 19.23+ parameters are:
    // "Ljava/lang/String;", // VideoId.
    // "[B",
    // "Ljava/lang/String;", // Player parameters proto buffer.
    // "Ljava/lang/String;", // PlaylistId.
    // "I",
    // "I",
    // "L",
    // "Ljava/util/Set;",
    // "Ljava/lang/String;",
    // "Ljava/lang/String;",
    // "L",
    // "Z", // Appears to indicate if the video id is being opened or is currently playing.
    // "Z",
    // "Z"
    customFingerprint = custom@{ methodDef, _ ->
        val parameterTypes = methodDef.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize != 13 && parameterSize != 14) {
            return@custom false
        }

        val startsWithMethodParameterList = parameterTypes.slice(0..5)
        val endsWithMethodParameterList = parameterTypes.slice(parameterSize - 7..<parameterSize)

        parametersEqual(STARTS_WITH_PARAMETER_LIST, startsWithMethodParameterList)
                && parametersEqual(ENDS_WITH_PARAMETER_LIST, endsWithMethodParameterList)
    }
) {
    val STARTS_WITH_PARAMETER_LIST = listOf(
        "Ljava/lang/String;", // VideoId.
        "[B",
        "Ljava/lang/String;", // Player parameters proto buffer.
        "Ljava/lang/String;", // PlaylistId.
        "I",
        "I"
    )
    val ENDS_WITH_PARAMETER_LIST = listOf(
        "Ljava/util/Set;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "L",
        "Z", // Appears to indicate if the video id is being opened or is currently playing.
        "Z",
        "Z"
    )

    fun parametersEqual(
        parameters1: Iterable<CharSequence>,
        parameters2: Iterable<CharSequence>
    ): Boolean {
        if (parameters1.count() != parameters2.count()) return false
        val iterator1 = parameters1.iterator()
        parameters2.forEach {
            if (!it.startsWith(iterator1.next())) return false
        }
        return true
    }
}