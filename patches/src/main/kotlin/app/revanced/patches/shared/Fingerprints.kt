package app.revanced.patches.shared

import app.revanced.patches.shared.extension.Constants.EXTENSION_SETTING_CLASS_DESCRIPTOR
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val createPlayerRequestBodyWithModelFingerprint = legacyFingerprint(
    name = "createPlayerRequestBodyWithModelFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.OR_INT_LIT16),
    customFingerprint = { method, _ ->
        indexOfModelInstruction(method) >= 0 &&
                indexOfReleaseInstruction(method) >= 0
    }
)

fun indexOfModelInstruction(method: Method) =
    method.indexOfFieldReference("Landroid/os/Build;->MODEL:Ljava/lang/String;")

fun indexOfReleaseInstruction(method: Method) =
    method.indexOfFieldReference("Landroid/os/Build${'$'}VERSION;->RELEASE:Ljava/lang/String;")

private fun Method.indexOfFieldReference(string: String) = indexOfFirstInstruction {
    val reference = getReference<FieldReference>() ?: return@indexOfFirstInstruction false

    reference.toString() == string
}

/**
 * On YouTube, this class is 'Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;'
 * On YouTube Music, class names are obfuscated.
 */
internal val formatStreamModelConstructorFingerprint = legacyFingerprint(
    name = "formatStreamModelConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.IGET_WIDE,
        Opcode.IPUT_WIDE,
    ),
    literals = listOf(45374643L),
)

internal val mdxPlayerDirectorSetVideoStageFingerprint = legacyFingerprint(
    name = "mdxPlayerDirectorSetVideoStageFingerprint",
    strings = listOf("MdxDirector setVideoStage ad should be null when videoStage is not an Ad state ")
)

internal val sharedSettingFingerprint = legacyFingerprint(
    name = "sharedSettingFingerprint",
    returnType = "V",
    customFingerprint = { method, _ ->
        method.definingClass == EXTENSION_SETTING_CLASS_DESCRIPTOR &&
                method.name == "<clinit>"
    }
)

internal val spannableStringBuilderFingerprint = legacyFingerprint(
    name = "spannableStringBuilderFingerprint",
    returnType = "Ljava/lang/CharSequence;",
    strings = listOf("Failed to set PB Style Run Extension in TextComponentSpec. Extension id: %s"),
    customFingerprint = { method, _ ->
        indexOfSpannableStringInstruction(method) >= 0
    }
)

const val SPANNABLE_STRING_REFERENCE =
    "Landroid/text/SpannableString;->valueOf(Ljava/lang/CharSequence;)Landroid/text/SpannableString;"

fun indexOfSpannableStringInstruction(method: Method) = method.indexOfFirstInstruction {
    opcode == Opcode.INVOKE_STATIC &&
            getReference<MethodReference>()?.toString() == SPANNABLE_STRING_REFERENCE
}

internal val startVideoInformerFingerprint = legacyFingerprint(
    name = "startVideoInformerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    strings = listOf("pc"),
    customFingerprint = { method, _ ->
        method.implementation
            ?.instructions
            ?.withIndex()
            ?.filter { (_, instruction) ->
                instruction.opcode == Opcode.CONST_STRING
            }
            ?.map { (index, _) -> index }
            ?.size == 1
    }
)

internal val videoLengthFingerprint = legacyFingerprint(
    name = "videoLengthFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("Gaplessly transitioning away from an Ad before it ends.")
)

internal val dislikeFingerprint = legacyFingerprint(
    name = "dislikeFingerprint",
    returnType = "V",
    strings = listOf("like/dislike")
)

internal val likeFingerprint = legacyFingerprint(
    name = "likeFingerprint",
    returnType = "V",
    strings = listOf("like/like")
)

internal val removeLikeFingerprint = legacyFingerprint(
    name = "removeLikeFingerprint",
    returnType = "V",
    strings = listOf("like/removelike")
)