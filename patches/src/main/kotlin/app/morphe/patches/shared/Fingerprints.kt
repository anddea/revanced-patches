/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Hoàng Gia Bảo (https://github.com/YT-Advanced)
 * - inotia00 (https://github.com/inotia00)
 * - rufusin (https://github.com/rufusin)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal const val ANDROID_AUTOMOTIVE_STRING = "Android Automotive"
internal const val CLIENT_INFO_CLASS_DESCRIPTOR =
    "Lcom/google/protos/youtube/api/innertube/InnertubeContext\$ClientInfo;"

internal val authenticationChangeListenerFingerprint = legacyFingerprint(
    name = "authenticationChangeListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    strings = listOf("Authentication changed while request was being made"),
    customFingerprint = { method, _ ->
        indexOfMessageLiteBuilderReference(method) >= 0
    }
)

internal fun indexOfMessageLiteBuilderReference(method: Method, type: String = "L") =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.parameterTypes?.isEmpty() == true &&
                reference.returnType.startsWith(type)
    }

internal val autoMotiveFingerprint = legacyFingerprint(
    name = "autoMotiveFingerprint",
    opcodes = listOf(
        Opcode.GOTO,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    ),
    strings = listOf(ANDROID_AUTOMOTIVE_STRING),
    customFingerprint = { _, classDef ->
        !classDef.type.startsWith("Lapp/")
    }
)

internal val buildRequestParentFingerprint = legacyFingerprint(
    name = "buildRequestParentFingerprint",
    returnType = "Ljava/util/Map;",
    strings = listOf("If-Modified-Since"),
)

internal val buildRequestFingerprint = legacyFingerprint(
    name = "buildRequestFingerprint",
    customFingerprint = { method, _ ->
        method.implementation != null &&
                indexOfNewUrlRequestBuilderInstruction(method) >= 0 &&
                // Earlier targets
                (indexOfEntrySetInstruction(method) >= 0 ||
                        // Later targets
                        method.parameters[1].type == "Ljava/util/Map;")
    }
)

internal const val BOLD_ICONS_FEATURE_FLAG = 45685201L

internal val boldIconsFeatureFlagMethodFingerprint = Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    filters = listOf(literal(BOLD_ICONS_FEATURE_FLAG))
)

internal fun indexOfNewUrlRequestBuilderInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>().toString() == "Lorg/chromium/net/CronetEngine;->newUrlRequestBuilder(Ljava/lang/String;Lorg/chromium/net/UrlRequest${'$'}Callback;Ljava/util/concurrent/Executor;)Lorg/chromium/net/UrlRequest${'$'}Builder;"
    }

internal fun indexOfEntrySetInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_INTERFACE &&
                getReference<MethodReference>().toString() == "Ljava/util/Map;->entrySet()Ljava/util/Set;"
    }

internal val clientTypeFingerprint = legacyFingerprint(
    name = "clientTypeFingerprint",
    opcodes = listOf(
        Opcode.IGET,
        Opcode.IPUT, // Sets ClientInfo.clientId.
    ),
    strings = listOf("10.29"),
    customFingerprint = { method, _ ->
        indexOfClientInfoInstruction(method) >= 0
    }
)

fun indexOfClientInfoInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.IPUT_OBJECT &&
                getReference<FieldReference>()?.type == CLIENT_INFO_CLASS_DESCRIPTOR
    }

internal val conversionContextFingerprintToString2 = legacyFingerprint(
    name = "conversionContextFingerprintToString2",
    parameters = emptyList(),
    strings = listOf(
        // YTM 6.20: "ConversionContext{container="
        // Others:   "ConversionContext{containerInternal="
        "ConversionContext{container",
        ", identifierProperty="
    ),
    customFingerprint = { method, _ ->
        method.name == "toString"
    }
)

internal val createPlayerRequestBodyWithModelFingerprint = legacyFingerprint(
    name = "createPlayerRequestBodyWithModelFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.OR_INT_LIT16),
    customFingerprint = { method, _ ->
        indexOfBrandInstruction(method) >= 0 &&
                indexOfManufacturerInstruction(method) >= 0 &&
                indexOfModelInstruction(method) >= 0 &&
                indexOfReleaseInstruction(method) >= 0 &&
                indexOfSdkInstruction(method) >= 0
    }
)

fun indexOfBrandInstruction(method: Method) =
    method.indexOfFieldReference("Landroid/os/Build;->BRAND:Ljava/lang/String;")

fun indexOfManufacturerInstruction(method: Method) =
    method.indexOfFieldReference("Landroid/os/Build;->MANUFACTURER:Ljava/lang/String;")

fun indexOfModelInstruction(method: Method) =
    method.indexOfFieldReference("Landroid/os/Build;->MODEL:Ljava/lang/String;")

fun indexOfReleaseInstruction(method: Method) =
    method.indexOfFieldReference("Landroid/os/Build${'$'}VERSION;->RELEASE:Ljava/lang/String;")

fun indexOfSdkInstruction(method: Method) =
    method.indexOfFieldReference("Landroid/os/Build${'$'}VERSION;->SDK_INT:I")

private fun Method.indexOfFieldReference(string: String) = indexOfFirstInstruction {
    val reference = getReference<FieldReference>() ?: return@indexOfFirstInstruction false

    reference.toString() == string
}

internal val createPlayerRequestBodyFingerprint = legacyFingerprint(
    name = "createPlayerRequestBodyFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.IGET,
        Opcode.AND_INT_LIT16,
    ),
    strings = listOf("ms"),
)

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

internal const val IS_DEFAULT_AUDIO_TRACK_STRING =
    "isDefaultAudioTrack="
internal const val AUDIO_TRACK_DISPLAY_NAME_STRING =
    "audioTrackDisplayName="
internal const val AUDIO_TRACK_ID_STRING =
    "audioTrackId="

/**
 * On YouTube, this class is 'Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;'
 * On YouTube Music, class names are obfuscated.
 */
internal val formatStreamModelToStringFingerprint = legacyFingerprint(
    name = "formatStreamModelToStringFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Ljava/lang/String;",
    strings = listOf(
        IS_DEFAULT_AUDIO_TRACK_STRING,
        AUDIO_TRACK_DISPLAY_NAME_STRING,
        AUDIO_TRACK_ID_STRING
    ),
    customFingerprint = { method, _ ->
        method.name == "toString"
    }
)

internal val mdxPlayerDirectorSetVideoStageFingerprint = legacyFingerprint(
    name = "mdxPlayerDirectorSetVideoStageFingerprint",
    strings = listOf("MdxDirector setVideoStage ad should be null when videoStage is not an Ad state ")
)

internal val spannableStringBuilderFingerprint = legacyFingerprint(
    name = "spannableStringBuilderFingerprint",
    returnType = "Ljava/lang/CharSequence;",
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.CONST_STRING &&
                    getReference<StringReference>()
                        ?.string.toString()
                        .startsWith("Failed to set PB Style Run Extension in TextComponentSpec.")
        } >= 0 &&
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

internal val videoLengthFingerprintLegacy = legacyFingerprint(
    name = "videoLengthFingerprintLegacy",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("Gaplessly transitioning away from an Ad before it ends.")
)

internal val videoLengthFingerprint = legacyFingerprint(
    name = "videoLengthFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("J", "J"),
    returnType = "V",
    literals = listOf(45633940L, 1000L),
    // strings = listOf("Gaplessly transitioning away from an Ad before it ends.")
    customFingerprint = { method, _ ->
        method.name == "a"
    }
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

internal val playbackStartParametersConstructorFingerprint = legacyFingerprint(
    name = "playbackStartParametersConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    opcodes = listOf(Opcode.IPUT_OBJECT)
)

internal const val FIXED_RESOLUTION_STRING = ", initialPlaybackVideoQualityFixedResolution="
internal const val WATCH_NEXT_RESPONSE_PROCESSING_DELAY_STRING =
    ", watchNextResponseProcessingDelay="

internal val playbackStartParametersToStringFingerprint = legacyFingerprint(
    name = "playbackStartParametersToStringFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Ljava/lang/String;",
    strings = listOf(
        FIXED_RESOLUTION_STRING,
        WATCH_NEXT_RESPONSE_PROCESSING_DELAY_STRING
    ),
    customFingerprint = { method, _ ->
        method.name == "toString"
    }
)
