package app.revanced.patches.youtube.general.livering

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val clientSettingEndpointFingerprint = legacyFingerprint(
    name = "clientSettingEndpointFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Ljava/util/Map;"),
    strings = listOf(
        "force_fullscreen",
        "PLAYBACK_START_DESCRIPTOR_MUTATOR",
        "VideoPresenterConstants.VIDEO_THUMBNAIL_BITMAP_KEY"
    ),
    customFingerprint = { method, _ ->
        indexOfPlaybackStartDescriptorInstruction(method) >= 0
    }
)

internal fun indexOfPlaybackStartDescriptorInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.returnType == "Lcom/google/android/libraries/youtube/player/model/PlaybackStartDescriptor;" &&
                reference.parameterTypes.isEmpty()
    }
